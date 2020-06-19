use serde::ser::{Serialize, Serializer, SerializeStruct};

#[derive(Clone)]
pub struct Branch {
    pub commit_id: String,
    pub name: String,
    pub is_head: bool,
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
