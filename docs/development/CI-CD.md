# Dead Archive - CI/CD Documentation

Complete guide to the automated build, test, and release pipeline for Dead Archive Android app.

## 🏗️ CI/CD Overview

The Dead Archive project uses **GitHub Actions** to provide a complete automated pipeline:

- 🔨 **Continuous Integration**: Build and test on every commit
- 🚀 **Continuous Deployment**: Automated releases with APK distribution
- 🧪 **Quality Assurance**: Automated testing and code quality checks
- 📱 **Artifact Management**: APK builds available for immediate download

## 📋 GitHub Actions Workflows

### 1. Build APK Workflow (`build-apk.yml`)

**Triggers:**
- Every push to `main` or `develop` branches
- Every pull request to `main`
- Manual dispatch

**What it does:**
```yaml
✅ Sets up Android development environment
✅ Builds debug and release APKs
✅ Runs unit tests and lint checks
✅ Uploads APK artifacts (30-90 day retention)
✅ Uploads test and lint reports
```

**Artifacts Created:**
- `dead-archive-debug-{commit}` - Debug APK for testing
- `dead-archive-release-{commit}` - Release APK (unsigned)
- `lint-reports-{commit}` - Code quality reports
- `test-reports-{commit}` - Unit test results

### 2. Release Workflow (`release.yml`)

**Triggers:**
- Git tags matching `v*.*.*` pattern (e.g., `v1.0.0`, `v2.1.3`)

**What it does:**
```yaml
✅ Builds production-ready APKs
✅ Runs comprehensive quality checks
✅ Generates changelog from git commits
✅ Creates GitHub release with APK downloads
✅ Generates SHA256 checksums for security
```

**Release Assets:**
- `dead-archive-{version}-release.apk` - Production APK
- `dead-archive-{version}-debug.apk` - Debug APK for testing
- `checksums.txt` - SHA256 verification hashes

### 3. Pull Request Check Workflow (`pr-check.yml`)

**Triggers:**
- Pull request opened, updated, or reopened

**What it does:**
```yaml
✅ Validates code builds successfully
✅ Runs all unit tests
✅ Performs lint analysis
✅ Comments results on PR
✅ Provides downloadable test APK
```

## 🚀 Getting Started

### Initial Repository Setup

1. **Create GitHub Repository**
   ```bash
   # On GitHub, create new repository: dead-archive
   # Don't initialize with README (we have one)
   ```

2. **Connect Local Repository**
   ```bash
   # Add GitHub as remote origin
   git remote add origin https://github.com/YOUR_USERNAME/dead-archive.git
   
   # Push all code and workflows
   git push -u origin main
   ```

3. **Update Badge URLs**
   Edit `README.md` and replace placeholder URLs:
   ```markdown
   # Change these URLs to match your repository
   [![Build APK](https://github.com/YOUR_USERNAME/dead-archive/actions/workflows/build-apk.yml/badge.svg)]
   [![Release](https://github.com/YOUR_USERNAME/dead-archive/actions/workflows/release.yml/badge.svg)]
   ```

4. **Verify Workflows**
   - Go to GitHub repository → **Actions** tab
   - First push automatically triggers build workflow
   - Build takes ~10 minutes initially (Android SDK download)

### First Release

```bash
# Create version tag for first release
git tag v1.0.0
git commit -m "chore: bump version to v1.0.0"

# Push tag to trigger release workflow
git push origin v1.0.0

# Check GitHub Releases page for downloadable APK
```

## 🔧 Workflow Configuration

### Environment Requirements

Each workflow sets up a complete Android build environment:

```yaml
- Ubuntu Latest runner
- JDK 17 (Temurin distribution)
- Android SDK with latest tools
- Gradle 8.14.2 (via gradle-build-action)
- Gradle caching for performance
- Automatic license acceptance
```

### Build Optimization

**Caching Strategy:**
- Gradle dependencies cached between builds
- Cache keys based on Gradle files for invalidation
- Significant speed improvement after first build

**Performance Features:**
- Parallel test execution
- Incremental builds where possible
- Artifact compression for faster uploads

### Security Measures

**APK Verification:**
- SHA256 checksums generated for all APKs
- Build reproducibility through fixed dependencies
- Secure artifact storage with GitHub

**Access Control:**
- Workflows use repository permissions
- No external secrets required for basic functionality
- Release creation requires push access to repository

## 📱 APK Distribution

### Download Locations

**For End Users:**
1. **GitHub Releases** (Recommended)
   - Go to: `https://github.com/YOUR_USERNAME/dead-archive/releases`
   - Download latest release APK
   - Verify with provided checksums

2. **GitHub Actions Artifacts**
   - Go to: Repository → Actions → Select workflow run
   - Download artifacts (requires GitHub account)
   - Temporary storage (30-90 days)

### Installation Instructions

**Android Device Setup:**
1. Enable "Install from unknown sources"
   - Settings → Security → Unknown Sources (Android 7-)
   - Settings → Apps → Special Access → Install Unknown Apps (Android 8+)

2. Download APK file
3. Open file manager and tap APK
4. Follow installation prompts

### APK Variants

- **Release APK**: Production version for end users
- **Debug APK**: Development version with debug symbols
- **Unsigned APK**: Requires manual signing for Play Store

## 🧪 Testing & Quality Assurance

### Automated Testing

**Unit Tests:**
```bash
# Local testing
make test

# Automated in CI
./gradlew test --stacktrace
```

**Lint Analysis:**
```bash
# Local linting
make lint

# Automated in CI
./gradlew lint --stacktrace
```

### Quality Gates

**Build Requirements:**
- ✅ All unit tests must pass
- ✅ No lint errors (warnings allowed)
- ✅ Successful APK compilation
- ✅ No build script failures

**Release Requirements:**
- ✅ All build requirements
- ✅ Proper version tag format
- ✅ Clean working directory
- ✅ All dependencies resolved

### Test Reports

**Available Reports:**
- **Unit Test Results**: HTML and XML format
- **Lint Analysis**: Detailed code quality metrics
- **Build Logs**: Complete compilation output
- **APK Analysis**: Size and composition details

## 🔄 Development Workflow

### Daily Development

```bash
# 1. Make code changes
vim app/src/main/java/...

# 2. Test locally
make run-emulator

# 3. Commit and push
git add .
git commit -m "feat: add new feature"
git push origin main

# 4. GitHub Actions automatically:
#    - Builds APK
#    - Runs tests
#    - Updates status badges
#    - Provides downloadable artifacts
```

### Feature Development

```bash
# 1. Create feature branch
git checkout -b feature/awesome-feature

# 2. Develop and test locally
make run-emulator

# 3. Push and create PR
git push origin feature/awesome-feature
# Create PR on GitHub

# 4. Automated PR checks:
#    - Build validation
#    - Test execution
#    - Lint analysis
#    - PR status comments
```

### Release Process

```bash
# 1. Ensure main branch is ready
git checkout main
git pull origin main

# 2. Update version (if needed)
# Edit app/build.gradle.kts version numbers

# 3. Create and push release tag
git tag v1.2.0
git push origin v1.2.0

# 4. GitHub automatically:
#    - Builds release APKs
#    - Creates GitHub release
#    - Generates changelog
#    - Provides download links
```

## 🚨 Troubleshooting

### Common Build Issues

**"SDK not found" Error:**
```yaml
# Solution: Workflow automatically installs Android SDK
# Check Actions logs for setup steps
```

**"Gradle wrapper" Error:**
```yaml
# Solution: Workflows use gradle-build-action instead of wrapper
# This ensures consistent Gradle version across all builds
# Local development can still use 'gradle' command directly
```

**"Tests failed" Error:**
```bash
# Local debugging
make test
./gradlew test --info

# Check test reports in GitHub Actions artifacts
```

**"Lint errors" Error:**
```bash
# Local debugging
make lint
./gradlew lint

# Fix errors, warnings are usually acceptable
```

### Workflow Debugging

**Check Build Logs:**
1. Go to GitHub → Actions → Failed workflow
2. Click on failed job
3. Expand failed step
4. Review error messages

**Download Artifacts:**
1. Go to workflow run page
2. Scroll to "Artifacts" section
3. Download relevant reports
4. Extract and analyze locally

### Performance Issues

**Slow Builds:**
- First build always slow (SDK download)
- Subsequent builds use cache (~3-5 minutes)
- Check cache hit rates in logs

**Failed Artifact Upload:**
- Usually network related
- Workflow will retry automatically
- Check artifact size limits

## 📊 Monitoring & Analytics

### Build Status Monitoring

**Status Badges:**
- Show real-time build health in README
- Green = passing, Red = failing
- Click badge to view workflow details

**Notifications:**
- GitHub sends email on build failures
- Configure in repository settings
- Set up Slack/Discord webhooks if needed

### Usage Analytics

**GitHub Insights:**
- Actions usage minutes
- Artifact storage usage
- Workflow execution frequency
- Success/failure rates

**Release Analytics:**
- Download counts per release
- Popular versions
- Platform distribution

## 🔧 Advanced Configuration

### Custom Build Variants

Add new build types in `app/build.gradle.kts`:
```kotlin
buildTypes {
    release { ... }
    debug { ... }
    staging {
        // Custom staging configuration
    }
}
```

Update workflows to build new variants:
```yaml
- name: Build staging APK
  run: ./gradlew assembleStaging
```

### Environment-Specific Builds

**Development Environment:**
```yaml
env:
  BUILD_TYPE: debug
  API_URL: https://api-dev.deadarchive.com
```

**Production Environment:**
```yaml
env:
  BUILD_TYPE: release
  API_URL: https://api.deadarchive.com
```

### External Integrations

**Slack Notifications:**
```yaml
- name: Notify Slack
  uses: 8398a7/action-slack@v3
  with:
    status: ${{ job.status }}
    webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

**Play Store Upload:**
```yaml
- name: Upload to Play Store
  uses: r0adkll/upload-google-play@v1
  with:
    serviceAccountJsonPlainText: ${{ secrets.SERVICE_ACCOUNT_JSON }}
    packageName: com.deadarchive.app
    releaseFiles: app/build/outputs/apk/release/app-release.apk
```

## 📚 Resources

### GitHub Actions Documentation
- [GitHub Actions Docs](https://docs.github.com/en/actions)
- [Android CI/CD Examples](https://github.com/actions/starter-workflows/tree/main/ci)
- [Workflow Syntax](https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions)

### Android Build Documentation
- [Android Gradle Plugin](https://developer.android.com/studio/build)
- [Build Configuration](https://developer.android.com/studio/build/build-variants)
- [Signing Your App](https://developer.android.com/studio/publish/app-signing)

### Best Practices
- [CI/CD for Android](https://developer.android.com/studio/build/building-cmdline)
- [Gradle Build Optimization](https://docs.gradle.org/current/userguide/build_environment.html)

---

*Generated for Dead Archive CI/CD Pipeline - Last updated: $(date)*