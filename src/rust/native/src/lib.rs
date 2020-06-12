use neon::prelude::*;
use neon::register_module;

use std::cmp::Ordering;

#[derive(Clone)]
struct Commit {
    id: git2::Oid,
    time: git2::Time,
    summary: String,
    message: String,
    author: git2::Signature<'static>,
    selected: bool,
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
            selected: false,
        }
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

fn local_branches(mut cx: FunctionContext) -> JsResult<JsArray> {
    let path = "/home/valerauko/Code/Kitsune/kitsune".to_string();
    let branches = match git2::Repository::open(&path) {
        Ok(repo) => match repo.branches(Some(git2::BranchType::Local)) {
            Ok(branches) => branches.fold(vec![], |mut aggr, branch| match branch {
                Ok((branch, _type)) => match branch.name() {
                    Ok(Some(name)) => {
                        aggr.push(name.to_string());
                        aggr
                    },
                    _ => aggr,
                },
                Err(_) => aggr,
            }),
            Err(_) => vec!["No branches".to_string()],
        },
        Err(_) => vec!["Couldn't open repo".to_string()],
    };

    let js_array = JsArray::new(&mut cx, branches.len() as u32);
    for (i, obj) in branches.iter().enumerate() {
        let name = cx.string(obj);
        js_array.set(&mut cx, i as u32, name).unwrap();
    }

    Ok(js_array)
}

register_module!(mut m, {
    m.export_function("localBranches", local_branches)?;
    m.export_function("commits", commits)?;
    Ok(())
});
