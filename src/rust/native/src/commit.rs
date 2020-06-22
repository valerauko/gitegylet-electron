use serde::ser::{Serialize, SerializeStruct, Serializer};

use std::cmp::Ordering;
use std::collections::{BinaryHeap, HashMap, HashSet};

use md5::{Digest, Md5};

#[derive(Clone)]
pub struct Commit {
    pub id: git2::Oid,
    time: git2::Time,
    summary: String,
    message: String,
    author: git2::Signature<'static>,
    pub parents: Vec<git2::Oid>,
    column: Option<usize>,
}

impl Commit {
    pub fn from_git2(commit: git2::Commit) -> Self {
        Self {
            id: commit.id(),
            time: commit.time(),
            summary: match commit.summary() {
                Some(summary) => summary.to_string(),
                None => commit.id().to_string(),
            },
            message: match commit.message() {
                Some(msg) => msg.to_string(),
                None => commit.id().to_string(),
            },
            author: commit.author().to_owned(),
            parents: commit.parent_ids().collect(),
            column: None,
        }
    }

    pub fn on_branches(repo: git2::Repository, branch_names: Vec<String>) -> Vec<Self> {
        let mut ids = HashSet::new();
        let mut heap = BinaryHeap::new();

        branch_names.iter().for_each(|name| {
            match repo.find_branch(name, git2::BranchType::Local) {
                Ok(branch) => match branch.get().peel_to_commit() {
                    Ok(commit) => {
                        if !ids.contains(&commit.id()) {
                            ids.insert(commit.id());
                            heap.push(Self::from_git2(commit));
                        }
                    }
                    Err(e) => println!("{}", e),
                },
                Err(e) => println!("{}", e),
            }
        });
        let mut commits: Vec<Commit> = vec![];

        if heap.is_empty() {
            return commits;
        }

        while commits.len() < 250 {
            match heap.pop() {
                Some(commit) => {
                    for parent_id in &commit.parents {
                        if !ids.contains(parent_id) {
                            match repo.find_commit(*parent_id) {
                                Ok(parent) => {
                                    ids.insert(*parent_id);
                                    heap.push(Self::from_git2(parent));
                                }
                                Err(e) => println!("{}", e)
                            }
                        }
                    };
                    commits.push(commit);
                }
                None => break,
            }
        }

        return commits;
    }

    pub fn listed(repo: git2::Repository, branch_names: Vec<String>) -> Vec<Self> {
        let commits = Self::on_branches(repo, branch_names);
        let mut commits_map = HashMap::with_capacity(commits.len());
        commits.iter().for_each(|commit| {
            commits_map.insert(commit.id, commit);
        });

        let commit_ids: Vec<git2::Oid> = commits.iter().map(|commit| commit.id).collect();

        let mut used_columns: HashSet<usize> = HashSet::new();
        let mut column_map: HashMap<git2::Oid, usize> = HashMap::new();

        column_mapper(
            &commits_map,
            &mut used_columns,
            &mut column_map,
            &commit_ids,
            true,
        );

        commit_ids
            .iter()
            .map(|id| {
                let mut commit = commits_map.get_mut(id).unwrap().clone();
                commit.column = Some(column_map[id]);
                commit
            })
            .collect()
    }
}

impl Serialize for Commit {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        let mut state = serializer.serialize_struct("Commit", 3)?;
        state.serialize_field("id", &self.id.to_string())?;
        state.serialize_field("summary", &self.summary)?;
        state.serialize_field("message", &self.message)?;
        state.serialize_field("timestamp", &self.time.seconds())?;

        let mut author = HashMap::new();
        let email = self.author.email().unwrap();
        author.insert("name", self.author.name().unwrap());
        author.insert("email", &email);
        let mut hasher = Md5::new();
        hasher.update(email.to_lowercase().as_bytes());
        let hash = format!("{:x}", hasher.finalize());
        author.insert("md5", &hash);
        state.serialize_field("author", &author)?;

        state.serialize_field(
            "parents",
            &self
                .parents
                .iter()
                .map(|parent| parent.to_string())
                .collect::<Vec<String>>(),
        )?;

        match self.column {
            Some(column) => state.serialize_field("column", &column)?,
            None => {}
        };

        state.end()
    }
}

impl Ord for Commit {
    fn cmp(&self, other: &Self) -> Ordering {
        self.time.cmp(&other.time)
    }
}

impl PartialOrd for Commit {
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        Some(self.cmp(other))
    }
}

impl PartialEq for Commit {
    fn eq(&self, other: &Self) -> bool {
        self.id == other.id
    }
}

impl Eq for Commit {}

fn first_unused(used_columns: &HashSet<usize>) -> usize {
    let mut col = 0usize;
    while used_columns.contains(&col) {
        col += 1;
    }
    return col;
}

fn column_mapper(
    commits_map: &HashMap<git2::Oid, &Commit>,
    mut used_columns: &mut HashSet<usize>,
    mut column_map: &mut HashMap<git2::Oid, usize>,
    ids: &Vec<git2::Oid>,
    should_recur_parents: bool,
) {
    for current_id in ids {
        let current_column = *column_map
            .entry(*current_id)
            .or_insert(first_unused(&used_columns));
        used_columns.insert(current_column);

        if let Some(commit) = commits_map.get(current_id) {
            if let Some(parent_id) = commit.parents.first() {
                if should_recur_parents && !column_map.contains_key(parent_id) {
                    column_map.insert(*parent_id, current_column);
                }
            }

            if !should_recur_parents {
                continue;
            }

            if commit.parents.len() > 1 {
                column_mapper(
                    &commits_map,
                    &mut used_columns,
                    &mut column_map,
                    &commit.parents,
                    false,
                );
            }

            if let Some(parent_id) = commit.parents.first() {
                match column_map.get(parent_id) {
                    Some(parent_col) => {
                        if *parent_col != current_column {
                            used_columns.remove(&current_column);
                        }
                    }
                    None => {}
                }
            }
        }
    }
}
