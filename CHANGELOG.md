# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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