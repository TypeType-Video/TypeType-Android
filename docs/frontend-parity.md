# Frontend parity

This matrix turns the TypeType web product into explicit Android work. It does
not require Android to copy the web layout. The frontend defines product
behavior, TypeType-Server defines the contract, and Android keeps a native
Compose interaction model.

## Audited revisions

The comparison was refreshed on 2026-08-06 from clean clones:

| Repository | Branch | Revision |
| --- | --- | --- |
| TypeType-Android | `dev` | `d192a4eda13287409ab7c98ab183398b70ba0856` |
| TypeType frontend | `dev` | `97103f7302c91accdea3322d1f5b8f610607f378` |
| TypeType-Server | `dev` | `6f31f93c4ecfb0e31c03c777b0d96f5cdc51f7bc` |
| TypeType-Web-Player | `dev` | `f2fe3e6976cd9beec603f324adf37edee923ee20` |

`Implemented` means a user can reach the native feature and the relevant
contract has automated coverage. `Partial` means useful behavior exists but a
visible path, state, or verification lane is missing. `Missing` is an accepted
Android gap, not an invitation to move a Server responsibility into the app.
`Adapted` records an intentional native boundary with its reason.

## Setup, accounts, and profile

| Frontend behavior | Server contract | Android state | Evidence or next requirement |
| --- | --- | --- | --- |
| F-Droid or release installation and first launch | None before setup | Partial | Debug cold launch is covered on API 23 through 37. The Release build embeds strict-stable Baseline and Startup profiles generated on API 37, plus ProfileInstaller for non-Play installs. For v1.2.3, the GitHub and F-Droid APKs are byte-identical, the published checksum verifies, the APK certificate matches the signer declared by the F-Droid index, and clean signed installs reach the same onboarding on API 23, 29, 34, and 37. The same signed upgrade, install, and parity checks remain mandatory for the final release from the current source |
| Instance discovery and compatibility | `/health`, `/instance` | Implemented | Setup repository and discovery contract tests |
| Local password login | `/auth/login`, `/auth/me`, `/auth/refresh` | Implemented | Scoped authenticator and account validation tests |
| Session renewal and reconnect | `/auth/refresh`, `/auth/me` | Implemented | A rejected refresh opens identity-bound reauthentication without deleting local account data, while transient failures preserve the session and cached content; a real expired session passed process restart on API 23, and the recovery surfaces pass on API 23 and API 37 |
| Guest access | `/auth/guest` | Implemented | Capability-driven login screen |
| External-browser OIDC | `/auth/oidc/start`, `/status`, `/callback` | Implemented | Real Keycloak journeys pass on API 23 AOSP without Google services and API 37, including browser fallback, callback, force-stop restoration, and cancellation cleanup |
| Password reset | `/auth/reset-password` | Implemented | Native reset screen and typed errors |
| Registration and first-admin bootstrap | `/auth/register/status`, `/auth/register` | Implemented | Native local/OIDC route, bootstrap redirect, Android contract tests, Server route tests, and API 33 Compose tests pass. A real beta API 29 process kill restored the registration route and its non-secret name and email draft while clearing the password |
| Reauthentication of the selected account | `/auth/login`, `/auth/oidc/callback` | Implemented | Account identity is checked before replacing credentials |
| Multiple instances and accounts | Account-scoped authenticated API | Implemented | Room scope, cookies, tokens, workers, cache, and navigation are isolated |
| Profile identity | `/profile`, `/profile/account` | Implemented | Native profile settings and identity Compose test |
| Emoji, custom, and reset avatar | `/profile/avatar/*` | Implemented | Picker, GIF decoder, upload DTO, and repository tests; authenticated beta accounts exercised WebP and animated GIF uploads, profile refresh, force-stop recreation, and reset on API 29, with successful upload, identity refresh, and delete responses; signed-release retest remains |
| YouTube session status and pairing | `/youtube-session/*` | Implemented | Native capability, status, authenticated remote browser, disconnect, expiry, and unavailable states have contract, unit, and API 33 Compose coverage. A real beta API 29 process kill restored the route and reloaded the current Server status |

## Discovery and browsing

| Frontend behavior | Server contract | Android state | Evidence or next requirement |
| --- | --- | --- | --- |
| Home recommendations | `/recommendations/home` | Implemented | Initial loading, fatal errors with request IDs, cached refresh, and empty states pass on API 23, 29, and 37 |
| Continue watching with progress | `/progress`, `/history` | Implemented | Native first section, durable progress outbox, accessible progress semantics, and stable video navigation pass on API 23 and API 37 |
| Trending fallback | `/trending` | Implemented | Home distinguishes fallback from recommendations |
| Subscription feed | `/subscriptions/feed` | Implemented | Progressive client, accessible loading, cached refresh, fatal error, empty state, retry status, and load harness pass on API 23, 29, and 37 |
| Subscribed channel directory | `/subscriptions` | Implemented | Native channels grid and provider tests |
| Channel videos, live, playlists, podcasts | `/channel/page`, `/channel/playlists`, `/podcasts` | Implemented | Continuation contracts, paging, loading, fatal error, empty, and cached-refresh states pass on API 23, 29, and 37 |
| Search suggestions and history | `/suggestions`, `/search-history` | Implemented | Account-scoped remote history and the native pre-search state pass on API 23, 29, and 37 |
| Search services, grouped filters, and pagination | `/search`, `/search/filters` | Implemented | Contract, selection, continuation, accessible loading, fatal error, and empty-result states pass on API 23, 29, and 37 |
| Public playlist detail and save | `/playlist`, `/saved-playlists` | Implemented | Native detail, pagination, save boundary, Library cache, and loading, fatal-error, and empty states pass on API 23, 29, and 37 |
| Podcast episodes and queue | `/podcasts/episodes` | Implemented | Paged detail, service-owned queue, and loading, fatal-error, and empty states pass on API 23, 29, and 37 |
| Shorts recommendations and subscriptions | `/recommendations/shorts`, `/subscriptions/shorts` | Partial | Native vertical pager, shared Media3 host, autoplay intent, favorite, Watch Later, comments, subscriptions, sharing, blocking, channel interleaving, explicit states, and bounded next-item metadata prefetch have contract, unit, and API 29 Compose coverage. A drag keeps the settled Short mounted and active until a different page settles, including cancelled swipes, so a transient gesture cannot pause the shared player. The actual pager and shared PlaybackService pass a 623-second standalone run across nine items, repeated seeks, an activity lifecycle transition, and two network changes on API 29; the smoke path also passes on API 23 without Google services. Member and guest beta accounts currently receive empty feed responses, so real Server-backed Shorts playback cannot yet be claimed |
| DeArrow titles and thumbnails | `/dearrow`, `/dearrow/thumbnail` | Implemented | Server-synchronized preferences, trusted-candidate resolution, shared card/player presentation, contract and Compose tests, plus a real beta process-recreation check |
| Block video, channel, and title keyword | `/blocked/*` | Implemented | Native menus/settings and contract tests |
| Notifications and unread count | `/notifications*` | Implemented | Screen, badge, read-all flow, initial loading, fatal error, empty state, DTO, and Compose tests pass on API 23, 29, and 37 |
| Web administration | Admin capabilities and routes | Adapted | The viewer exposes native bug reporting and account controls; deployment, user moderation, allow-list, and Server administration remain in the web console |

## Library, import, and downloads

| Frontend behavior | Server contract | Android state | Evidence or next requirement |
| --- | --- | --- | --- |
| History with paging, search, date filters, and deletion | `/history` | Implemented | Server-side query and date bounds, native today/week/month/day filters, per-item removal, confirmed clear-all, scoped Room refresh, contract and Compose tests on API 23, 29, and 37, plus a real beta surface check |
| Favorites and watch later | `/favorites`, `/watch-later` | Implemented | Durable desired-state outbox and cached tabs |
| Playlist create, rename, delete, reorder | `/playlists*` | Implemented | Native dialogs, rollback, summary/detail separation, cached-refresh retention, and loading, fatal-error, and empty states pass on API 23, 29, and 37 |
| Saved public playlists | `/saved-playlists` | Implemented | Dedicated cached Library tab |
| TypeType backup export and restore | `/backup/typetype`, `/restore/typetype` | Implemented | Android document picker and multipart contract tests |
| PipePipe restore | `/restore/pipepipe` | Implemented | ZIP validation, summary, refresh, and contract tests |
| YouTube Takeout import | `/imports/youtube-takeout*` | Partial | Native multi-archive queue, long-running scoped work, persisted Server jobs, preview/report states, cache refresh, contract tests, and Room tests pass. The queued job survives a real database close and reopen on API 23 and API 37, and the native import states pass on both versions. The Server does not advertise this capability and interrupted multipart uploads cannot resume, so a real beta import remains blocked by the contract rather than hidden behind a client fallback |
| Server download selection and jobs | `/downloader/jobs*` | Implemented | Native format sheet, WorkManager observation, typed errors |
| Completed artifact transfer | Signed artifact URL | Partial | API 29, API 34, and an Android 16 runtime with 16 KiB pages complete and reconcile a real local DownloadManager transfer. API 23 also recovers the completed system download after a full Android reboot and an in-place APK replacement with data preserved. The signed-release retest remains |

## Watch and playback

| Frontend behavior | Server contract | Android state | Evidence or next requirement |
| --- | --- | --- | --- |
| Provider playback | `/streams` and Server media URLs | Implemented | Decoder-aware Media3 source selection |
| YouTube VOD SABR | `/streams/youtube/sabr/bootstrap`, `/sabr/playback/*` | Implemented | Shared lifecycle, bounded windows, recovery, and generation tests |
| Live and DVR playback | Shared SABR live window | Partial | A real beta live ran for more than 13 minutes on API 29 through forward and backward DVR seeks, background and foreground transitions, and a 60-second outage. A separate 150-second pause exhausted multiple SABR sessions; Android rebuilt the native playback binding, recovered without a process restart, and remained stable for three minutes. Active-live and post-live Server contracts, window conversion, and Media3 timeline tests pass; a real live-to-replay transition remains |
| Audio-only playback | SABR audio target | Implemented | The device-owned default mirrors the frontend's local behavior and applies once when the matching VOD and service command are ready. Manual changes remain authoritative for the current video across mini-player navigation, while the next video receives the default again; service command, default policy, settings, seek, and audio-only contract/coordinator tests cover the path |
| Quality, codec, audio, subtitle, and speed selection | Stream and playback contracts | Implemented | Native sheet state and selection tests |
| Server-proxied YouTube subtitles | `/subtitles/youtube/{videoId}` | Implemented | TTML endpoint mapping, overlay, cue loader, manual, automatic, translated-track, seek, and bounded-failure device tests pass on API 23, 29, and 37. The Media3 overlay preserves cue geometry and applies the synchronized caption appearance in portrait, fullscreen, Shorts, and PiP. A real beta TED video reached the native track selector and preserved playback when the Server returned the typed 429 unavailable state; visual caption success from that endpoint remains blocked by the current beta egress |
| SponsorBlock actions and chapters | Stream segment metadata and settings | Implemented | Category policy, skip/mute/manual notice, chapter and Compose tests |
| Queue, autoplay, shuffle, and repeat | Stable video identities | Implemented | One service-owned queue and persistence tests |
| Sleep timer | Device-owned Media3 state | Implemented | Service timer and device tests |
| Keep the display awake during video playback | Device window state | Implemented | The window flag follows the single MediaSession playback state in portrait, fullscreen, and Shorts. Its lifecycle passes on API 23, 29, and 37; on API 29, a real beta VOD stayed awake in portrait and fullscreen with a five-second system timeout. A second run with a 15-second timeout kept the display on for 36 idle seconds while playback advanced, then pausing removed the flag and let the display sleep normally |
| Stable video surface during transient resets | Shared MediaSession and media identity | Implemented | Retained-content, lifecycle, and refresh-gate tests pass on API 23, 29, and 37, so a track or activity reset does not restore a black shutter or create a competing surface |
| Exact seek and scrub behavior | `/sabr/playback/*` | Implemented | Cancelled drags never seek and completed drags commit the final timestamp on API 23, 29, and 37. Repeated requests start immediately, stale results cannot replace the newest generation, and long continuity runs include bidirectional seeks |
| Comments and replies | `/comments`, `/comments/replies` | Implemented | Paging source and native bottom sheet |
| Related videos and channel actions | Stream metadata and subscriptions | Implemented | Shared video menus and player channel state |
| Phone, tablet, fullscreen, mini-player, PiP | MediaSession and device capabilities | Partial | Adaptive watch layout is tested. The API 23 AOSP lane without Google services has a 10-minute local H.264 run through the shared MediaSession with eight bidirectional seeks, and a current-app launch proving PiP code remains gated when the platform feature is absent. On API 29 AOSP, the real System UI play and pause actions control the same service-owned session without starting another process or activity; live playback exposes only the valid playback action, while VOD also exposes audio-only. API 34 covers H.264 PiP, notification permission, and foreground-service publication. Fullscreen rotation and restoration pass on API 23 and 29. The API 37 16 KiB enforcing image passes the PiP action contract, task-isolated transition, fullscreen window restoration, and all 202 non-codec tests; five H.264 playback scenarios are excluded because the enforcing emulator image rejects its codec path. Rotating that image also crashes its Goldfish graphics mapper inside `system_server`, so neither result is presented as physical Android 17 evidence. Signed PiP and physical Android 17 codec and rotation retests remain |
| Network loss and reconnection | Fresh Server session and bounded retry | Partial | Recovery distinguishes validated, blocked, and suspended routes and ignores capability-only noise. A random beta VOD played for more than 12 minutes on API 29 through bidirectional seeks, app-state transitions, and a 63-second outage; cached media played until depletion and the same session resumed about 9 seconds after reconnection. A real beta live passed the equivalent 60-second outage and recovered within 8 seconds. Signed-release and post-live network verification remain |
| Playback restoration after process death | Stable video identity, `/progress` | Implemented | Room close/reopen restores exact position, queue, repeat mode, and account scope without persisting signed media URLs |
| Danmaku overlay | `/bullet-comments` | Implemented | Capability-aware contract, bounded lane scheduling, native overlay, speed and size controls, TalkBack suppression, and Compose/performance tests |

## Settings and product states

| Frontend behavior | Android state | Evidence or next requirement |
| --- | --- | --- |
| Appearance, theme, and accent | Implemented | Device-scoped native settings |
| Player defaults and caption style | Implemented | Autoplay countdown and the start-in-audio-only choice remain device-owned because the Server has no fields for them; synchronized player defaults continue through Server settings. All nine frontend-compatible caption fields are exposed through native controls, resolved to the same defaults, bounded before rendering, and applied to the shared Media3 subtitle path. Unit and device tests pass on API 23 without Google services, API 29, and API 37; API 29 visual checks cover both the settings and a rendered positioned cue |
| SponsorBlock preferences | Implemented | Server-synchronized controls and policy tests |
| Content visibility and history privacy | Implemented | Server-synchronized controls and write gates |
| DeArrow preferences | Implemented | Native content controls cover title, thumbnail, and trust modes; a real beta search displayed the Server title and representative frame |
| Diagnostics review, export, and clear | Implemented | Bounded redacted local store and sanitizer tests |
| Native bug report | Implemented | Preview and explicit submission path; pinned Server retest remains |
| Loading, cached refresh, empty, partial, and fatal states | Implemented | Home, Shorts, Notifications, Subscriptions, Search, Channel, Podcast, public-playlist detail, Library, and local-playlist detail exercise accessible loading, fatal errors with request IDs, explicit empty states, and cached refresh where applicable on API 23, 29, and 37 |
| Font scale, TalkBack, RTL, keyboard, D-pad, foldable | Implemented | The top-level shell passes 200% text, RTL ordering, compact and wide navigation, landscape, and D-pad focus traversal on API 23, 29, and 37. The full settings destination list remains reachable at 200% text, every interactive settings control exposes spoken text, and keyboard activation works in RTL on the same matrix. Android Accessibility Suite TalkBack was bound with touch exploration on the API 37 16 KiB enforcing image: setup, Home, Shorts, Subscriptions, Library, Search, Profile, the player, and all ten settings routes each exposed interactive controls without an unnamed action or editable field. The speech pipeline received the focused setup instance field, and the current APK passed the same 37 targeted accessibility and Shorts tests on API 23 without Google services, API 29, and API 37. On an API 34 foldable, closed, half-open, and opened postures each passed the shell and player layout suites; the real app remained resumed with the same process across posture changes and a 1080 x 2340 outer-display to 1768 x 2208 inner-display resize |

## Deliberate platform differences

- The browser embed route stays a web concern. Android accepts video deep links
  and uses the same native player instead of embedding a second player path.
- The web admin console remains a deployment-management surface. It is not part
  of the Android viewer contract unless a separate mobile administration goal
  is approved.
- The frontend's navigation and cinema presentation are references for behavior,
  not layouts to copy. Android keeps bottom navigation, a rail on wide windows,
  one MediaSession, system PiP, and platform caption/accessibility behavior.
- The frontend prepares two upcoming Shorts queries. Android keeps one
  metadata-only candidate, checks Data Saver, and never prefetches media
  segments or a SABR session. This bounds mobile and Server work while keeping
  the next item ready.
- Extraction, YouTube credentials, SABR policy, PO tokens, DeArrow resolution,
  recommendations, imports, and downloader jobs remain Server-owned.

## Release gate

An item moves from `Partial` to `Implemented` only after its contract, native UI,
failure states, process recreation, and applicable API lanes are exercised. The
final release still requires API 23 through 37 evidence, long random VOD and
live playback with seek and network changes, signed upgrade/install checks,
checksum and signature verification, and issue-by-issue reproduction results.
The profile integration is verified inside the unsigned Release APK; startup
latency improvements remain unclaimed until a physical-device benchmark runs.
