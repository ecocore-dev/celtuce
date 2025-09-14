# CONTRIBUTING

Thanks for contributing! Please follow these rules to keep the project maintainable.

## Git Flow

- `master` contains **production-ready code only**.  
  Direct commits to `master` are **not allowed**.  
- Every change goes into a **feature branch** created from `master` (e.g. `codec-resp3`).  
- Open a **PR from your feature branch to `master`**.  
- Keep your branch updated via **`git rebase origin/master`**, not merge commits.  
- Force-push is allowed **only to feature branches**, never to `master`.  
- **Require linear history**: PRs are merged with **Rebase & Merge**, preserving individual commits but avoiding merge commits.

---

## Commit Messages

We follow [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/).  

Format:
```
<type>(<scope>)!: <short summary>

[optional body]

[optional footer(s)]
```

### Allowed `<type>` values
- `feat` – new feature  
- `fix` – bug fix  
- `refactor` – code change without behavior change  
- `perf`, `docs`, `test`, `chore`, `ci`, `build`, `revert`  

Use `!` or `BREAKING CHANGE:` footer for incompatible changes.  

### Examples
- `feat(codec): support RESP3 bulk strings`  
- `fix(redis): handle MOVED errors`  
- `refactor(kondo): unify config`  

> All **commits inside a PR** must follow Conventional Commits. Since we do not squash, the **full commit history is preserved**.

---

## Versioning & Releases

We follow [Semantic Versioning](https://semver.org/):  
- **MAJOR**: incompatible API changes (`BREAKING CHANGE`)  
- **MINOR**: backwards-compatible features (`feat`)  
- **PATCH**: backwards-compatible bug fixes (`fix`, `perf`)  

Changelog and release notes are generated from commit history.  

---

## PR Guidelines

- Keep PRs small and focused.  
- Document motivation, changes, and breaking notes.  
- Tests should be updated or added where possible.  
- CI must be green before merging.  

---

🙌 Thanks for helping improve the project!  
