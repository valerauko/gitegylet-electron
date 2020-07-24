use neon::prelude::*;
use neon::register_module;

use crate::{branch::Branch, commit::Commit, status::Status};

mod branch;
mod commit;
mod status;

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
        .map(|js_value| js_value.downcast::<JsString>().unwrap().value())
        .collect();

    let js_path: Handle<JsString> = cx.argument(0)?;
    let repo_path: String = js_path.downcast::<JsString>().unwrap().value();
    let repo = git2::Repository::open(&repo_path).unwrap();

    let commits = Commit::listed(repo, branches);

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

fn fetch(mut cx: FunctionContext) -> JsResult<JsUndefined> {
    let arr_handle: Handle<JsArray> = cx.argument(1)?;
    let js_array: Vec<Handle<JsValue>> = arr_handle.to_vec(&mut cx)?;
    let branch_names: Vec<String> = js_array
        .iter()
        .map(|js_value| js_value.downcast::<JsString>().unwrap().value())
        .collect();
    let js_path: Handle<JsString> = cx.argument(0)?;
    let repo_path: String = js_path.downcast::<JsString>().unwrap().value();
    let repo = git2::Repository::open(&repo_path).unwrap();

    Branch::fetch(repo, branch_names);

    Ok(cx.undefined())
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

fn checkout_branch(mut cx: FunctionContext) -> JsResult<JsUndefined> {
    let js_path: Handle<JsString> = cx.argument(0)?;
    let repo_path: String = js_path.downcast::<JsString>().unwrap().value();
    let repo = git2::Repository::open(&repo_path).unwrap();

    let js_branch_name: Handle<JsString> = cx.argument(1)?;
    let branch_name: String = js_branch_name.downcast::<JsString>().unwrap().value();
    let branch = Branch::by_name(&repo, &branch_name);

    branch.checkout(&repo);

    Ok(cx.undefined())
}

fn create_branch(mut cx: FunctionContext) -> JsResult<JsUndefined> {
    let js_path: Handle<JsString> = cx.argument(0)?;
    let repo_path: String = js_path.downcast::<JsString>().unwrap().value();
    let repo = git2::Repository::open(&repo_path).unwrap();

    let js_commit_id: Handle<JsString> = cx.argument(1)?;
    let commit_id: String = js_commit_id.downcast::<JsString>().unwrap().value();

    let js_name: Handle<JsString> = cx.argument(2)?;
    let name: String = js_name.downcast::<JsString>().unwrap().value();

    Branch::create(&repo, &commit_id, &name);

    Ok(cx.undefined())
}

fn commit_diff(mut cx: FunctionContext) -> JsResult<JsArray> {
    let js_path: Handle<JsString> = cx.argument(0)?;
    let repo_path: String = js_path.downcast::<JsString>().unwrap().value();
    let repo = git2::Repository::open(&repo_path).unwrap();

    let js_commit_id: Handle<JsString> = cx.argument(1)?;
    let commit_id: String = js_commit_id.downcast::<JsString>().unwrap().value();

    let files = Commit::diff_files(repo, commit_id);

    let js_array = JsArray::new(&mut cx, files.len() as u32);
    for (i, status) in files.iter().enumerate() {
        let js_status = neon_serde::to_value(&mut cx, &status)?;
        js_array.set(&mut cx, i as u32, js_status)?;
    }

    Ok(js_array)
}

fn statuses(mut cx: FunctionContext) -> JsResult<JsArray> {
    let js_path: Handle<JsString> = cx.argument(0)?;
    let repo_path: String = js_path.downcast::<JsString>().unwrap().value();
    let repo = git2::Repository::open(&repo_path).unwrap();

    let statuses = Status::all(repo);

    let js_array = JsArray::new(&mut cx, statuses.len() as u32);
    for (i, status) in statuses.iter().enumerate() {
        let js_status = neon_serde::to_value(&mut cx, &status)?;
        js_array.set(&mut cx, i as u32, js_status)?;
    }
    Ok(js_array)
}

register_module!(mut m, {
    m.export_function("localBranches", local_branches)?;
    m.export_function("checkoutBranch", checkout_branch)?;
    m.export_function("createBranch", create_branch)?;
    m.export_function("fetch", fetch)?;
    m.export_function("commits", commits)?;
    m.export_function("commitDiff", commit_diff)?;
    m.export_function("statuses", statuses)?;
    m.export_function("head", head)?;
    Ok(())
});
