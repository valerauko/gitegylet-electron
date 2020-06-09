use neon::prelude::*;
use neon::register_module;

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

register_module!(mut m, { m.export_function("lastCommit", last_commit) });
