# TinyKeePass

Another simple read-only KeePass Android app.

Only what I personally need are implemented, lacks lots of common functions, and possibly buggy.

[Google Play](https://play.google.com/store/apps/details?id=org.sorz.lab.tinykeepass)

## Features

* Fetch/update database from HTTP(S)/WebDav server, support HTTP basic auth.

* Remember master password, which is protected by
  [Android Keystore System](https://developer.android.com/training/articles/keystore.html).
  (Cannot not-to-remember password, though)

* Fingerprint or screen lock support.

* Copy username and/or password to clipboard, with counting down on notification.

* Searching.

* Autofill on Oreo (experimental)

* No support: any edit/delete operations, groups, display fields other than title, username, password, and URL.

* Android >= 7.0 (N)


## Credits

 * [KeePass](http://keepass.info/):
 the free, open source, light-weight and easy-to-use password manager.
 
 * [openkeepass](https://github.com/cternes/openkeepass/):
 a java library for reading and writing KeePass databases
