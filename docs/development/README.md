# Development Documentation

This directory contains operational documentation for Dead Archive development and deployment.

## 📚 Documentation Files

### [CI-CD.md](CI-CD.md) - Continuous Integration & Deployment
**Complete guide to automated build and release pipeline**
- GitHub Actions workflows
- Build artifact management
- Release process automation
- Quality assurance checks

### [RELEASE_BUILD_GUIDE.md](RELEASE_BUILD_GUIDE.md) - Release Build Process  
**Step-by-step release building and signing**
- Keystore creation and management
- Signing configuration setup
- Release APK and AAB generation
- Production build troubleshooting

## 🔗 Related Documentation

- **[../SETUP.md](../SETUP.md)** - Development environment setup
- **[../architecture/](../architecture/)** - System architecture documentation
- **[../testing.md](../testing.md)** - Testing strategy and implementation

## 🚀 Quick References

### Build Commands (from project root)
```bash
make run-emulator        # Complete development workflow
make build              # Build debug APK
make release            # Build release APK
make test               # Run unit tests
make lint               # Code quality checks
```

### Release Process
```bash
make tag-release        # Create release with quality checks
git tag v1.0.0 && git push origin v1.0.0  # Trigger automated release
```

### CI/CD Pipeline
- **Every commit**: Automatic build and test
- **Tagged releases**: APK generation and GitHub release
- **Pull requests**: Automated quality checks

---

For development environment setup, see the main [SETUP.md](../SETUP.md) guide.