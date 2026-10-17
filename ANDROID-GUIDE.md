# LOAF on Android: setup guide

How it works: the **LOAF app** is a thin shell. It opens your normal LOAF page (the one on GitHub Pages) full screen, and adds one thing a browser can't do: it books every bell, nudge, meeting notice and the day recap with the phone's alarm system. So they ring on time with LOAF closed and the phone locked, and one live countdown notice stays on screen from Start day to the finish line.

The web app stays the only copy of LOAF. A normal upload of `index.html` updates the app too. You only build a new app file when the `android/` folder changes.

Do the parts in order. Part 1 and Part 2 are done once.

---

## Part 1 · Upload the files to GitHub (10 min)

Upload these to the LOAF repo (the one that serves GitHub Pages):

| File or folder | What to do |
|---|---|
| `index.html` | replace the old one (it now says v42 at the bottom) |
| `sw.js` | replace the old one (it now says `loaf-v42`; that is what makes installed copies pick up the update) |
| `README.md` | replace the old one (optional, it mentions the app) |
| `ANDROID-GUIDE.md` | **new**, this guide |
| `android/` folder | **new**, the app's source |
| `.github/workflows/build-loaf-app.yml` | **new**, tells GitHub how to build the app |

On GitHub: open the repo → **Add file → Upload files** → drag in the files, the `android` folder **and** the hidden `.github` folder (on a Mac press Cmd+Shift+. in Finder to show it) → **Commit changes**.

If your browser won't take the hidden folder: **Add file → Create new file**, type `.github/workflows/build-loaf-app.yml` as the name, paste in that file's contents, and commit.

GitHub Pages carries on serving LOAF as before. In a browser, LOAF v42 behaves exactly like v41.

---

## Part 2 · Add the signing key (5 min)

Every copy of the app is signed with one key. Android only installs an update over the top (keeping Haz's day, jar and thoughts) if it's signed with the **same** key, so this key must never change.

You have two private files (in the project folder, `private/`): `LOAF_KEYSTORE.txt` (a long block of letters) and `LOAF_KEY_PASSWORD.txt`.

1. GitHub repo → **Settings → Secrets and variables → Actions → New repository secret**.
2. Name `LOAF_KEYSTORE`, value: the whole contents of `LOAF_KEYSTORE.txt`. **Add secret**.
3. Again: name `LOAF_KEY_PASSWORD`, value: the contents of `LOAF_KEY_PASSWORD.txt`. **Add secret**.
4. **Keep a copy of both files somewhere safe** (a password manager, or a private drive). GitHub won't show a secret again. If they're lost, the next app can only be installed after uninstalling the old one, which wipes Haz's data. Then delete them from the project folder.

---

## Part 3 · Build the app (5 min)

1. Repo → **Actions** tab. If it asks, click **I understand my workflows, go ahead and enable them**.
2. **Build LOAF app** → **Run workflow** → leave the address empty → **Run workflow**.
   It finds LOAF's GitHub Pages address by itself and prints it in the log ("LOAF's address: …"). If yours is unusual, type the full address in the box instead.
3. After about 4–6 minutes click the finished run → under **Artifacts** download **LOAF-app**. It's a zip; inside is `LOAF-1.apk` (the number goes up with each build).

---

## Part 4 · Install it on Haz's phone (10 min)

1. Unzip and send `LOAF-N.apk` to the phone (Google Drive, email to themselves, or USB). Open it on the phone.
2. Android asks to allow installs from that app (Drive, Gmail or Files). Allow it, go back, tap **Install**. Play Protect may say it doesn't recognise the developer: choose **More details → Install anyway**.
3. Open **LOAF**. Tap Start day (or Start now). When Android asks about notifications, tap **Allow**.
4. **Battery:** Settings → Apps → LOAF → Battery → **Unrestricted**. On a Samsung, also Settings → Battery → Background usage limits: make sure LOAF isn't in **Sleeping apps** or **Deep sleeping apps**. Some brands' battery savers hold alarms back otherwise.
5. **Haz's data doesn't come across by itself.** The app has its own storage, separate from Chrome's LOAF, so it starts with an empty jar and no thoughts. Redo the settings (the gear) once. The Chrome copy keeps its data; it just isn't shared.

**Try it:** tap Start now, lock the phone, and wait for the bell after 25 minutes. Or use a short Timebox. The live notice should tick on the lock screen the whole time.

**What to expect:**
- One notice, "LOAF", stays at the top while the day runs. It shows the countdown and ticks on its own. Its buttons change: **Pause** while a bit runs, **Start focus / Start break** and **Snooze 5 min** while Lumi waits, **Ended** for a meeting, **Carry on** when paused.
- At each moment (a bell, a nudge, a meeting running over) it changes its words and rings once. Between moments it's silent.
- "Coming up" before a meeting pops up as its own notice.
- At the finish line it turns into the day recap.
- While a bell is booked, a small alarm-clock icon shows in the status bar. That's how Android makes sure the bell is never late.
- Snooze rings again 5 min later without opening LOAF. The other buttons open LOAF and do the thing.
- Android 16 phones may show the countdown as a small chip in the status bar.
- In Android settings → Apps → LOAF → Notifications there are five kinds you can tune: Live countdown, Bells, Meetings, Nudges, Day recap.

---

## Updates later

- **Changes to LOAF itself** (almost all of them): upload `index.html` and `sw.js` as usual. The app picks them up next time it opens with internet.
- **Changes to the app shell** (only when you're sent a new `android/` folder): upload it, run **Build LOAF app** again, and install the new APK over the top. Same key, so nothing is lost.

## Play Store and developer verification

Not needed. Sideloading is free. From 2027, Android will ask that sideloaded apps are registered to a verified developer; a free "limited distribution" account (up to 20 devices) covers this, and it's a one-off registration of the app.

---

## If something goes wrong

| Problem | Try |
|---|---|
| The workflow says the secrets are missing | Part 2: the names must be exactly `LOAF_KEYSTORE` and `LOAF_KEY_PASSWORD`. |
| The workflow can't find LOAF's address | Type the full address (starting `https://`) in the box when you run it. |
| "App not installed" when updating | It was signed with a different key. Check the secrets are the same ones as before. |
| The app says Lumi can't reach LOAF | It needs internet the first time it opens, and tries again every 5 seconds by itself. |
| Bells are late with the phone locked | Part 4 step 4: battery Unrestricted, and not a sleeping app. |
| No notices at all | Settings → Apps → LOAF → Notifications → on. LOAF's own settings (the gear) will say if they're blocked. |
| The live notice is gone | Swiping it away puts it back. If it's still gone, open LOAF once. |

---
A Bouncy Production · Made for Haz
