# dynamic_datasource
When your data source is read from a database or obtained dynamically, you might need this, similar to a data extraction tool.
# process
1. We take over Spring's database connection pool and managing the database connection pool ourselves; we are using the Druid connection pool here.
2. Set up a primary data source to store main information, such as your configured data source information and other data information you need to save.
3. Based on the unique key of the configured data source, switch to the specified data source to perform data queries or operations.
4. Switching back to the main datasource is a crucial step and cannot be omitted.

