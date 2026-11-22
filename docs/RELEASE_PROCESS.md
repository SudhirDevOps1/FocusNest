# FocusNest — Official Release Process

## 1. Conventional Commits
All pull requests and commits merged to `main` must follow the Conventional Commits specification:
- `feat:` Introduces a new feature (triggers minor bump).
- `fix:` Fixes a bug (triggers patch bump).
- `docs:` Documentation changes only.
- `style:` Formatting or UI styling without logic changes.
- `refactor:` Code improvements without behavioral changes.
- `perf:` Performance improvements.
- `test:` Adding or fixing tests.
- `ci:` CI/CD pipeline updates.
- `chore:` Maintenance tasks.

---

## 2. Release Please Workflow
1. When a PR is merged into `main`, GitHub Actions runs the `release-please` workflow.
2. Release Please analyzes commit messages and maintains an open Release Pull Request (e.g. `chore(main): release 0.1.0-alpha.1`).
3. When the maintainer approves and merges the Release PR:
   - A new Git tag (e.g. `v0.1.0-alpha.1`) is created automatically.
   - A GitHub Release is drafted.
   - The `publish-release-apk` workflow triggers on the tagged commit, builds signed release APKs and AAB bundles, generates `SHA256SUMS.txt`, and attaches them to the release.
