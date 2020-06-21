use serde::ser::{Serialize, SerializeStruct, Serializer};
use std::collections::{HashMap, HashSet};

#[derive(Clone)]
pub struct Branch {
    commit_id: String,
    name: String,
    is_head: bool,
    ahead_behind: Option<(usize, usize)>,
}

impl Branch {
    pub fn from_git2(repo: &git2::Repository, branch: git2::Branch) -> Result<Self, git2::Error> {
        let name = match branch.name() {
            Ok(Some(name)) => name.to_string(),
            Ok(None) => return Err(git2::Error::from_str("Invalid branch name")),
            Err(e) => return Err(e),
        };
        let commit_id = branch.get().peel_to_commit()?.id();

        let ahead_behind = match branch.upstream() {
            Ok(upstream) => match upstream.get().peel_to_commit() {
                Ok(commit) => match repo.graph_ahead_behind(commit_id, commit.id()) {
                    Ok(ab) => Some(ab),
                    Err(e) => {
                        println!("{}", e);
                        None
                    }
                },
                Err(e) => {
                    println!("{}", e);
                    None
                }
            },
            Err(e) => {
                println!("{}", e);
                None
            }
        };

        Ok(Self {
            commit_id: commit_id.to_string(),
            name,
            is_head: branch.is_head(),
            ahead_behind,
        })
    }

    pub fn locals(repo: git2::Repository) -> Vec<Self> {
        let branches = repo.branches(Some(git2::BranchType::Local)).unwrap();
        branches.fold(vec![], |mut aggr, branch| match branch {
            Ok((branch, _type)) => match branch.name() {
                Ok(Some(name)) => match branch.get().peel_to_commit() {
                    Ok(commit) => {
                        aggr.push(Self {
                            commit_id: commit.id().to_string(),
                            is_head: branch.is_head(),
                            name: name.to_string(),
                            ahead_behind: None,
                        });
                        aggr
                    }
                    Err(_) => aggr,
                },
                _ => aggr,
            },
            Err(_) => aggr,
        })
    }

    pub fn fetch(repo: git2::Repository, branch_names: Vec<String>) -> Vec<Self> {
        let mut remotes = HashSet::new();
        let mut branch_by_remote: HashMap<String, Vec<String>> = HashMap::new();

        branch_names.iter().for_each(|name| {
            match repo.find_branch(name, git2::BranchType::Local) {
                Ok(branch) => match branch.upstream() {
                    Ok(upstream) => match upstream.name() {
                        Ok(Some(upstream_name)) => {
                            let pieces: Vec<&str> = upstream_name.splitn(2, "/").collect();
                            let remote_name = pieces[0].to_string();
                            let branch_name = pieces[1].to_string();
                            remotes.insert(remote_name.clone());
                            (*branch_by_remote.entry(remote_name).or_insert(vec![]))
                                .push(branch_name);
                        }
                        Err(e) => println!("{}", e),
                        _ => {}
                    },
                    Err(e) => println!("{}", e),
                },
                Err(e) => println!("{}", e),
            }
        });

        remotes
            .iter()
            .for_each(|remote_name| match repo.find_remote(remote_name) {
                Ok(mut remote) => match remote.fetch(&branch_by_remote[remote_name], None, None) {
                    Err(e) => println!("{}", e),
                    _ => {}
                },
                Err(e) => println!("{}", e),
            });

        branch_names.iter().fold(vec![], |mut aggr, branch_name| {
            match repo.find_branch(branch_name, git2::BranchType::Local) {
                Ok(branch) => match Branch::from_git2(&repo, branch) {
                    Ok(branch) => {
                        aggr.push(branch);
                        aggr
                    }
                    Err(e) => {
                        println!("{}", e);
                        aggr
                    }
                },
                Err(e) => {
                    println!("{}", e);
                    aggr
                }
            }
        })
    }
}

impl Serialize for Branch {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        let mut state = serializer.serialize_struct("Branch", 3)?;
        state.serialize_field("commitId", &self.commit_id.to_string())?;
        state.serialize_field("name", &self.name)?;
        state.serialize_field("isHead", &self.is_head)?;
        match self.ahead_behind {
            Some(ahead_behind) => state.serialize_field("aheadBehind", &ahead_behind)?,
            None => {}
        };
        state.end()
    }
}