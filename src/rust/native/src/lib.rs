use neon::prelude::*;
use neon::register_module;

use serde::ser::{Serialize, Serializer, SerializeStruct};

use std::cmp::Ordering;
use std::collections::{BinaryHeap, HashSet};

#[derive(Clone)]
struct Commit {
    id: git2::Oid,
    time: git2::Time,
    summary: String,
    message: String,
    author: git2::Signature<'static>,
}

impl Commit {
    fn from_git2(commit: git2::Commit) -> Self {
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
        }
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

fn commits(mut cx: FunctionContext) -> JsResult<JsArray> {
    let js_path: Handle<JsString> = cx.argument(0)?;
    let repo_path: String = js_path.downcast::<JsString>().unwrap().value();

    let arr_handle: Handle<JsArray> = cx.argument(1)?;
    let js_array: Vec<Handle<JsValue>> = arr_handle.to_vec(&mut cx)?;
    let branches: Vec<String> = js_array
        .iter()
        .map(|js_value| {
            js_value
                .downcast::<JsString>()
                .unwrap()
                .value()
        })
        .collect();

    let repo = git2::Repository::open(&repo_path).unwrap();

    let mut ids = HashSet::new();
    let mut heap = BinaryHeap::new();

    branches
        .iter()
        .for_each(|name| match repo.find_branch(name, git2::BranchType::Local) {
            Ok(branch) => match branch.get().peel_to_commit() {
                Ok(commit) => {
                    ids.insert(commit.id());
                    heap.push(Commit::from_git2(commit));
                },
                Err(e) => println!("{}", e)
            },
            Err(e) => println!("{}", e)
        });

    if heap.is_empty() {
        return Ok(cx.empty_array());
    }

    let mut commits: Vec<Commit> = vec![];
    while commits.len() < 100 {
        match heap.pop() {
            Some(commit) => {
                repo.find_commit(commit.id)
                    .unwrap()
                    .parents()
                    .for_each(|parent| {
                        if !ids.contains(&parent.id()) {
                            ids.insert(parent.id());
                            heap.push(Commit::from_git2(parent));
                        }
                    });
                commits.push(commit);
            }
            None => break,
        }
    }

    let js_commits = JsArray::new(&mut cx, commits.len() as u32);
    for (i, commit) in commits.iter().enumerate() {
        let message = cx.string(&commit.summary);
        js_commits.set(&mut cx, i as u32, message).unwrap();
    }
    Ok(js_commits)
}

#[derive(Clone)]
struct Branch {
    commit_id: String,
    name: String,
    is_head: bool,
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
        state.end()
    }
}

fn local_branches(mut cx: FunctionContext) -> JsResult<JsArray> {
    let js_path: Handle<JsString> = cx.argument(0)?;
    let repo_path: String = js_path.downcast::<JsString>().unwrap().value();

    let branches = match git2::Repository::open(&repo_path) {
        Ok(repo) => match repo.branches(Some(git2::BranchType::Local)) {
            Ok(branches) => branches.fold(vec![], |mut aggr, branch| match branch {
                Ok((branch, _type)) => match branch.name() {
                    Ok(Some(name)) => {
                        match branch.get().peel_to_commit() {
                            Ok(commit) => {
                                aggr.push(Branch {
                                    commit_id: commit.id().to_string(),
                                    is_head: branch.is_head(),
                                    name: name.to_string(),
                                });
                                aggr
                            },
                            Err(_) => aggr,
                        }
                    },
                    _ => aggr,
                },
                Err(_) => aggr,
            }),
            Err(_) => vec![],
        },
        Err(_) => vec![],
    };

    let js_array = JsArray::new(&mut cx, branches.len() as u32);
    for (i, branch) in branches.iter().enumerate() {
        let js_branch = neon_serde::to_value(&mut cx, &branch)?;
        js_array.set(&mut cx, i as u32, js_branch)?;
    }

    Ok(js_array)
}

register_module!(mut m, {
    m.export_function("localBranches", local_branches)?;
    m.export_function("commits", commits)?;
    Ok(())
});
