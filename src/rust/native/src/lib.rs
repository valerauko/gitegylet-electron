use neon::prelude::*;
use neon::register_module;

mod branch;
mod commit;

use crate::{branch::Branch, commit::Commit};

fn head(mut cx: FunctionContext) -> JsResult<JsValue> {
    let js_path: Handle<JsString> = cx.argument(0)?;
    let repo_path: String = js_path.downcast::<JsString>().unwrap().value();
    let repo = git2::Repository::open(&repo_path).unwrap();

    let head = repo.head().unwrap().peel_to_commit().unwrap();
    let js_head = neon_serde::to_value(&mut cx, &Commit::from_git2(head))?;
    Ok(js_head)
}

fn commits(mut cx: FunctionContext) -> JsResult<JsArray> {
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

    let js_path: Handle<JsString> = cx.argument(0)?;
    let repo_path: String = js_path.downcast::<JsString>().unwrap().value();
    let repo = git2::Repository::open(&repo_path).unwrap();

    let commits = Commit::on_branches(repo, branches);

    if commits.is_empty() {
        return Ok(cx.empty_array());
    }

    let js_commits = JsArray::new(&mut cx, commits.len() as u32);
    for (i, commit) in commits.iter().enumerate() {
        let js_commit = neon_serde::to_value(&mut cx, &commit)?;
        js_commits.set(&mut cx, i as u32, js_commit).unwrap();
    }
    Ok(js_commits)
}

fn local_branches(mut cx: FunctionContext) -> JsResult<JsArray> {
    let js_path: Handle<JsString> = cx.argument(0)?;
    let repo_path: String = js_path.downcast::<JsString>().unwrap().value();
    let repo = git2::Repository::open(&repo_path).unwrap();

    let branches = Branch::locals(repo);

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
    m.export_function("head", head)?;
    Ok(())
});
