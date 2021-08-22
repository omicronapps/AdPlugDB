# AdPlugDb

> AdPlug song database

## Description

[AdPlugDb](https://github.com/omicronapps/AdPlugDb.git) is a song database for the [AdPlug](http://adplug.github.io/) sound player library. Database control is managed through an application service, with interfaces provided for database control and various callbacks. A separate helper class is also provided, for managing service lifecycle and monitoring the connection state.

AdPlugDb is used in [AndPlug](https://play.google.com/store/apps/details?id=com.omicronapplications.andplug) music player application for Android devices.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Testing](#testing)
- [Usage](#usage)
- [Example](#example)
- [Credits](#credits)
- [Release History](#release-history)
- [License](#license)

## Prerequisites

- [Android 4.0.3](https://developer.android.com/about/versions/android-4.0.3) (API Level: 15) or later (`ICE_CREAM_SANDWICH_MR1`)
- [Android Gradle Plugin](https://developer.android.com/studio/releases/gradle-plugin) 7.0.1) or later (`gradle:7.0.1`)

## Installation

Setup steps:

1. Check out a local copy of AdPlugDb repository
2. Build library with Gradle, using Android Studio or directly from the command line 

```
$ git clone https://github.com/omicronapps/AdPlugDb.git
```

## Testing

AdPlugDb includes instrumented unit tests.

### Instrumented tests

Located under `adplugdb/src/androidTest`.

These tests are run on a hardware device or emulator, and verifies correct operation of the 'AdPlugDb' implementation as accessed through `AdPlugDbService`. A set of songs need to be downloaded and installed in order  to run the instrumented tests.

Setup steps:
```
cd AdPlugDB/adplugdb/src/androidTest/assets/
wget http://modland.com/pub/modules/Ad%20Lib/EdLib%20D00/MSK/en%20lille%20test.d00 en_lille_test_d00 
wget http://modland.com/pub/modules/Ad%20Lib/EdLib%20D00/MSK/fresh.d00 fresh_d00
wget http://modland.com/pub/modules/Ad%20Lib/EdLib%20D00/JCH/gone.d00 gone_d00
wget http://modland.com/pub/modules/Ad%20Lib/EdLib%20D00/Drax/coop-Metal/super%20nova.d00 super_nova_d00
wget http://modland.com/pub/modules/Ad%20Lib/EdLib%20D00/JCH/the%20alibi.d00 the_alibi_d00
```

## Usage

AdPlugDb is controlled through the following class and interfaces:
- `AdPlugDbController` - service management class 
- `IAdPlugDb` - database service interface
- `IAdPlugDbCallback` - callback interface

### `AdPlugDbController`

Manages an instance of the `AdPlugDbController` application service.

#### Constructor

```AdPlugDbController(IAdPlugDbCallback callback, Context context)```

Constructor for the `AdPlugDbController` class.

Arguments:
- `callback` - allows registering for callbacks through the  `IAdPlugDbCallback` interface
- `context` - required in order for `AdPlugDbController` to manage the application service

#### `startDB`

```void startDB()```

Create and connect (bind) to `AdPlugDbService` application service. While the service is running, `AdPlugDbController` monitors its state, and provides callbacks to a registered listener through `IAdPlugDbCallback` interface.

#### `stopDB`

```void stopDB()```

Disconnect (unbind) from the application service.

#### `getService`

```IAdPlugDb getService()```

Returns a reference to a `AdPlugDbService` instance. Only valid between `onServiceConnected()` and `onServiceDisconnected()` callbacks of `IAdPlugDbCallback`!

### `IPlayer`

#### getStatus

```void getStatus()```

Get current database status. Status returned through callback `onStatus()`.

#### index

```void index(String root)```

Recursively index all songs and folders under provided path. Indexing completed when `onStatus()` callback returns `dbStatus.INITIALIZED`. Calls to `list(String path)` are allowed while database is being initialized, but may result in list with some songs not yet indexed.

- `path` - path to root folder

#### delete

```void delete()```

Delete database. Deletion completed when `onStatus()` callback returns `dbStatus.UNINITIALIZED`.

#### list

```void list(String path, int sortby, int order, boolean quick, boolean hide, boolean random)```

Get list of songs under provided path. Result returned through callback `onList(List<AdPlugFile> songs)`.

- `path` - path to folder
- `sortby` - sort criterion
- `order` - sort order
- `quick` - use existing database
- `hide` - hide unsupported files
- `random` - shuffle list

#### playlist

```void playlist()```

Get list of all playlists songs. Result returned through callback `onPlaylist(List<AdPlugFile> playlists)`.

#### add

```void add(String song, long length)```

Add song to database.

- `song` - full path and name of song
- `length` - file length

#### remove

```void remove(String song)```

Remove song from database.

- `song` - full path and name of song

#### getCount

```void getCount()```

Get number of entries (songs and folders) in database. Count returned through callback `onGetCount()`.

#### onSongInfo

```void onSongInfo(String song, String type, String title, String author, String desc, long length, long songlength, int subsongs, boolean valid, boolean playlist)```

Reverse callback from application to provide information on song, when so requested through `requestInfo()`.

### `IAdPlugDbCallback`

Callback interface from `AdPlugDb` database instance.

#### onServiceConnected

```void onServiceConnected()```

Application service connected. `AdPlugDbController.getService()` may be used to retrieve a reference to a `AdPlugDbService` instance following this callback.

#### onServiceDisconnected

```void onServiceDisconnected()```

Application service disconnected. Any reference to the `AdPlugDbService` instance is now invalid.

#### onStatusChanged

```void onStatusChanged(dbStatus status)```

Notification callback of new `AdPlugDb` state.

- `state` - current state

#### requestInfo

```void requestInfo(String name, long length)```

Request from `AdPlugDb` instance for AdPlug song information.

- `name` - full path and name of song
- `length` - file length

#### onList

```void onList(List<AdPlugFile> songs)```

Callback from `AdPlugDb` with list of songs, following request through `list()`.

- `songs` - list of songs in requested folder

#### onPlaylist

```void onPlaylist(List<AdPlugFile> playlists)```

Callback from `AdPlugDb` with list of playlists, following request through `playlist()`.

- `playlists` - list of all playlists

#### onStatus

```void onStatus(dbStatus status)```

Callback from `AdPlugDb` with database status, following request through `getStatus()`.

- `status` - current database status

#### onGetCount

```void onGetCount(long count)```

Callback from `AdPlugDb` with number of entries, following request through `getCount()`.

- `count` - number of entries (songs and folders) in database

## Example

Implement `IAdPlugDbCallback` callback interface:

```
import com.omicronapplications.adplugdb.IAdPlugDbCallback;
import com.omicronapplications.adplugdb.IAdPlugDb;

class AdPlugDbCallback implements IAdPlugDbCallback {
    @Override
    public void onServiceConnected() {
        AdPlugDbController controller;
        // Bound to AdPlugDbService, retrieve IAdPlugDb instance
        IAdPlugDb service = controller.getService();
    }

    @Override
    public void onServiceDisconnected() {
        // Unbound from AdPlugDbService, IAdPlugDb instance unusable
    }

    @Override
    public void onStatusChanged(dbStatus status) {
    }

    @Override
    public void requestInfo(String name, long length) {
    }

    @Override
    public void onList(List<AdPlugFile> songs) {
    }

    @Override
    public void onStatus(dbStatus status) {
    }

    @Override
    public void onGetCount(long count) {
    }
}
```

Create an `AdPlugDbController` instance to bind to `AdPlugDbService`:

```
import com.omicronapplications.adplugdb.AdPlugDbController;

IAdPlugDbCallback callback = new AdPlugDbCallback();
AdPlugDbController controller = new AdPlugDbController(callback, getApplicationContext());
controller.startDB();
```

Retrieve `IAdPlugDb` object on `IAdPlugDbCallback.onServiceConnected()` callback, and index songs in folder:

```
import com.omicronapplications.adplugdb.IAdPlugDb;

AdPlugDbController controller;
IAdPlugDb service = controller.getService();
service.index(dir.getAbsolutePath());
```

Request list of songs in folder:

```
service.list(dir.getAbsolutePath());
```

Destroy `AdPlugDbController` instance to unbind from `AdPlugDbService`:

```
controller.destroy();
```

## Credits

Copyright (C) 2020-2021 [Fredrik Claesson](https://github.com/omicronapps)

## Release History

- 1.0.0 Initial release
- 1.1.0 Support for quick listings and for hiding invalid files from list
- 1.2.0 Extended sort options
- 1.3.0 Added support for retrieving playlists and random shuffle

## License

AdPlugDb is licensed under [GNU LESSER GENERAL PUBLIC LICENSE](LICENSE).
