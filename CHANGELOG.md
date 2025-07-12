# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.7.0] - 12-07-2025
### Changed
- Updated libraries


## [2.6.8] - 12-10-2023
### Fixed
- Startup crash on Android 14


## [2.6.7] - 11-10-2023
### Fixed
- Fixed permission issues on Android 13
- Startup crash on Android 14

### Changed
- Updated libraries


## [2.6.6] - 08-06-2023
### Changed
- Updated libraries


## [2.6.5] - 22-07-2022
### Fixed
- Fixed crash on startup on some devices


## [2.6.4] - 19-07-2022
### Changed
- Updated libraries


## [2.6.3] - 07-08-2021
### Fixed
- Fixed startup crash


## [2.6.2] - 06-08-2021
### Fixed
- Minor bug fixes

### Changed
- Updated third party libraries


## [2.6.1] - 07-10-2020
### Changed
- `rpc-listen-all` is set to `false` by default when using the InAppDownloader


## [2.6.0] - 07-07-2020
### Added
- Added list of interfaces

### Changed
- Load CA certificates instead of disabling check


## [2.5.3] - 09-04-2020
### Added
- Added feature to import/export configuration and session


## [2.5.2] - 17-03-2020
### Changed
- Using Android logging utils
- Fixed F-Droid not picking up app details


## [2.5.1] - 05-02-2020
### Added
- Added dialog about native libraries not being extracted

### Fixed
- Fixed crash at startup
- Fixed crash reporting and analytics (Google Play only)
- Fixed billing system so that payments aren't refunded


## [2.5.0] - 05-01-2020
### Added
- Support for Android TV


## [2.4.3] - 22-12-2019
### Added
- Start service when app opens

### Changed
- Migrated to Firebase (Google Play users only)


## [2.4.2] - 01-12-2019
### Changed
- Fixed permission denied on Android 10 devices


## [2.4.1] - 29-11-2019
### Changed
- Fixed aria2 not working on some devices


## [2.4.0] - 27-11-2019
### Changed
- aria2 binary is now bundled inside the APK for compatibility reasons


## [2.3.0] - 07-10-2019
### Added
- Added support for x86 devices and expanded support for ARM devices


## [2.2.0] - 01-10-2019
### Changed
- Updated Material Design


## [2.1.7] - 19-08-2019
### Changed
- Fixed crash due to aria2 service


## [2.1.6] - 18-08-2019
### Changed
- Moved aria2c execution on service thread 
- Fixed NPE crash due to service


## [2.1.5] - 22-07-2019
### Added
- Debug logs for service crash