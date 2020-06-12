use neon::prelude::*;
use neon::register_module;

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

fn last_commit(mut cx: FunctionContext) -> JsResult<JsString> {
    let path = "/home/valerauko/Code/Kitsune/kitsune".to_string();
    let message = match git2::Repository::open(&path) {
        Ok(repo) => match repo.head() {
            Ok(head) => match head.peel_to_commit() {
                Ok(commit) => match commit.summary() {
                    Some(msg) => msg.to_string(),
                    None => commit.id().to_string(),
                },
                Err(_) => "HEAD is not a commit".to_string(),
            },
            Err(_) => "No HEAD".to_string(),
        },
        Err(_) => "Couldn't open repo".to_string(),
    };
    Ok(cx.string(format!("The last commit {}.", message)))
}

register_module!(mut m, {
    m.export_function("lastCommit", last_commit)?;
    m.export_function("localBranches", local_branches)?;
    Ok(())
});
