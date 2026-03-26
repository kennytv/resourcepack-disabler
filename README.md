# Resource Pack Disabler

Lets you prevent backend resource packs from being sent to players of certain Minecraft versions,
globally or per Velocity server.

```yaml
# Example values. Allows exact versions, ranges, and comparison operators: > / < / >= / <=
global:
  - 26.1
servers:
  lobby:
    - 1.9-1.12.2
  modern:
    - '>=1.21'
```
