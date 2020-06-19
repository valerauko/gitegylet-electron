use serde::ser::{Serialize, Serializer, SerializeStruct};

use std::cmp::Ordering;
use std::collections::{HashMap};

use md5::{Md5, Digest};

#[derive(Clone)]
pub struct Commit {
    pub id: git2::Oid,
    pub time: git2::Time,
    pub summary: String,
    pub message: String,
    pub author: git2::Signature<'static>,
    pub parents: Vec<git2::Oid>,
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
                .collect::<Vec<String>>()
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
