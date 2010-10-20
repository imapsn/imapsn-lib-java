# Issues

This is a list of issues with the current implementation

These will become todo's after some thought about how to address them.

## Exchange of contact information

When a friend request is made personal information is sent in the
clear via email. Any intermediate system involved in delivering the
mail has access to the personal data. 

How to address? The friend request/accept sequence needs to be broken
into three steps:

    * u1 sends request to u2 that only contains name, email, and,
      public key
    * u2 sends response encrypted with u1's public key. Response
      contains u2's full contact data including u2's public key.
    * u1 sends acknowledgment encrypted with u2's public
      key. Acknowledgement contains u1's full contact data.

## Transactions

There aren't any. There is no concurrency either, so isolation is not
a worry. (Is this true? Will there ever be a reason to have multiple
connections to one account?) 

Since there is no mechanism to make a series of writes atomic, there
will be consistency issues. 

Approach 1: 

For each operation think about whether it is idempotent, and if not
come up with compensating actions that will take care of the failure
cases.

Approach 2:

  Client uses it's own local transactional data store and the data is
  serialized to the IMAPSN folder periodically.  Then the only
  function of the IMAPSN folder is to store a back up of the data and
  allow one to switch clients.

# The key-map and status-map

Right now when a news-item is processed the person-status-map.json is
updated and saved to the imap store. When processing a long list of
incoming news items it doesn't makes sense to require serialization of
the whole status map for each news item processed.

Idea: 

  * replace `/key-map.json` with `/key/{key-hash}`
  * replace `/person-status-map.json` with `/status/{person.id}`



