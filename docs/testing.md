# DeadArchive Testing Strategy

This document outlines the comprehensive testing strategy for the DeadArchive application, with a focus on automated testing and specific approaches for testing Archive.org integration.

## Testing Layers

Our testing strategy consists of multiple layers to ensure thorough coverage:

1. **Unit Tests**: Test individual components in isolation
2. **Integration Tests**: Test interaction between components
3. **Repository Tests**: Test data access layer with mock data sources
4. **End-to-End Tests**: Test complete features without network dependencies
5. **UI Tests**: Test user interface components

## Test Categories

### Core Module Tests

The core modules contain most of our business logic and should have extensive test coverage:

- **core/model**: Data model unit tests
- **core/network**: API and networking tests using fixtures
- **core/data**: Repository and data source tests
- **core/media**: Media playback tests with mock player implementations

### Feature Module Tests

Feature modules contain UI components and feature-specific logic:

- **Concert browsing**: Test listing and filtering logic
- **Media playback**: Test playback UI and integration with core media module
- **Search functionality**: Test search algorithm and result display

## Archive.org Integration Testing

Archive.org integration is critical to the application functionality. We use fixture-based testing to ensure reliability:

1. **Fixture Sources**:
   - Real API responses collected from Archive.org
   - Error cases and edge cases
   - Responses from different show formats and eras

2. **Test Coverage Areas**:
   - URL construction
   - Metadata parsing
   - Track URL extraction
   - Error handling
   - Retry mechanisms
   - Response validation

3. **Mock Network Layer**:
   - Replace network calls with fixture responses
   - Simulate various network conditions
   - Test timeout handling

See the [Archive.org Test Fixtures](archive_test_fixtures.md) document for detailed implementation.

## Test Implementation

### Dependency Injection for Testing

We use Hilt to facilitate dependency injection in tests:

```kotlin
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [NetworkModule::class]
)
object TestNetworkModule {
    @Provides
    @Singleton
    fun provideArchiveApiService(): ArchiveApiService {
        return MockArchiveApiService()
    }
}
```

### JUnit Test Structure

Our test classes follow this general structure:

```kotlin
@RunWith(AndroidJUnit4::class)
class ArchiveRepositoryTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var repository: ArchiveRepository
    private lateinit var mockApiService: MockArchiveApiService
    
    @Before
    fun setup() {
        mockApiService = MockArchiveApiService()
        repository = ArchiveRepositoryImpl(mockApiService)
    }
    
    @Test
    fun testSpecificFunctionality() {
        // Given
        // Define test inputs
        
        // When
        // Execute method being tested
        
        // Then
        // Assert expected outcomes
    }
}
```

### Testing Coroutines

For testing code with coroutines:

```kotlin
@ExperimentalCoroutinesApi
class CoroutineTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()
    
    @Test
    fun testSuspendFunction() = runTest {
        // Test body
    }
}
```

## Integration with Existing Tests

The new fixture-based tests integrate with the existing test suite in several ways:

1. **Compatible Structure**: Follow the same patterns and conventions
2. **Shared Resources**: Use common test utilities and helper classes
3. **Same Build Process**: Run as part of the same test task in Gradle
4. **Consistent Assertions**: Use the same assertion libraries and patterns

## Test Data Management

### Fixture Collection

To collect fixtures for Archive.org responses:

```bash
# Create directories if they don't exist
mkdir -p core/network/src/test/resources/fixtures/metadata
mkdir -p core/network/src/test/resources/fixtures/responses

# Collect metadata for a concert
curl "https://archive.org/metadata/gd1990-04-03.158672.UltraMatrix.sbd.cm.miller.t.flac1644" \
  > core/network/src/test/resources/fixtures/metadata/gd1990-04-03.158672.UltraMatrix.sbd.cm.miller.t.flac1644.json

# Collect sample error responses
curl -i "https://archive.org/metadata/nonexistent-item" \
  > core/network/src/test/resources/fixtures/responses/404_response.txt
```

### Sample Fixture Data

Sample fixtures would contain actual Archive.org JSON responses, such as:

```json
{
  "created": 1669043777,
  "d1": "ia801509.us.archive.org",
  "d2": "ia601509.us.archive.org",
  "dir": "/1/items/gd1990-04-03.158672.UltraMatrix.sbd.cm.miller.t.flac1644",
  "files": [
    {
      "name": "02 Shakedown Street.mp3",
      "source": "original",
      "track": "2",
      "size": "17670352",
      "format": "MP3",
      "md5": "8610c58b9bb117cb31e605b46376e204",
      "title": "Shakedown Street",
      "length": "12:17"
    },
    // Additional files...
  ],
  "files_count": 52,
  "item_size": 2976363101,
  "metadata": {
    "identifier": "gd1990-04-03.158672.UltraMatrix.sbd.cm.miller.t.flac1644",
    "title": "Grateful Dead Live at The Omni on 1990-04-03",
    "creator": "Grateful Dead",
    "venue": "The Omni",
    "coverage": "Atlanta, GA",
    "date": "1990-04-03"
  },
  "server": "ia801509",
  "uniq": 556584848,
  "workable_servers": ["ia801509.us.archive.org"]
}
```

## Running Tests

Tests can be run through:

- **Gradle command line**: `./gradlew test`
- **Android Studio**: Use the test runner UI
- **CI/CD pipeline**: Automated test execution on commit

## Test Reporting

Test results are reported in multiple formats:

- JUnit XML reports for CI/CD integration
- HTML reports for human readability
- Test coverage reports with JaCoCo

## Test Maintenance

To keep tests up to date and reliable:

1. **Add New Fixtures**: When new Archive.org shows are supported or when errors are encountered
2. **Update Existing Fixtures**: When Archive.org changes their API or response format
3. **Refactor Tests**: When application code changes structure or functionality
4. **Review Coverage**: Regularly check test coverage and add tests for uncovered code

## Recommended Test Improvements

1. **Automated Fixture Collection**: Create a tool to automatically collect and organize fixtures
2. **Parameterized Tests**: Use JUnit parameterized tests to run the same test with different fixtures
3. **Network Condition Simulation**: Add tests that simulate poor network conditions
4. **Visual Test Reports**: Generate visual reports of test coverage and results