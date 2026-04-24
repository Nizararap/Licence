# DroidForge License Control

This file is used by the DroidForge app to check its status.

## How it works

The app checks `license.json` once every 24 hours.  
If the request fails (no internet / GitHub down), the app **still runs** (fail-open).

## To disable the app

Change `license.json` to:

```json
{
  "active": false,
  "message": "DroidForge has been discontinued. Thank you for using it!"
}
```

## To re-enable

Change back to:

```json
{
  "active": true,
  "message": ""
}
```

The `message` field is shown to users when `active` is false.  
Leave it empty `""` when active is true.
