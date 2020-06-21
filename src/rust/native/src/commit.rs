use serde::ser::{Serialize, SerializeStruct, Serializer};

use std::cmp::Ordering;
use std::collections::{BinaryHeap, HashMap, HashSet};

use md5::{Digest, Md5};

#[derive(Clone)]
pub struct Commit {
    id: git2::Oid,
    time: git2::Time,
    summary: String,
    message: String,
    author: git2::Signature<'static>,
    parents: Vec<git2::Oid>,
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
                                    heap.push(Commit::from_git2(parent));
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
