# Frontend parity

This matrix turns the TypeType web product into explicit Android work. It does
not require Android to copy the web layout. The frontend defines product
behavior, TypeType-Server defines the contract, and Android keeps a native
Compose interaction model.

## Audited revisions

The comparison was refreshed on 2026-08-05 from clean clones:

| Repository | Branch | Revision |
| --- | --- | --- |
| TypeType-Android | `dev` | `976679011a6e4265a59a19ff1c974ea08d0bab6c` |
| TypeType frontend | `dev` | `97103f7302c91accdea3322d1f5b8f610607f378` |
| TypeType-Server | `dev` | `6f31f93c4ecfb0e31c03c777b0d96f5cdc51f7bc` |
| TypeType-Web-Player | `main` | `f4844c6e65d021c0ad1ab61169f4f51cfcd77694` |

`Implemented` means a user can reach the native feature and the relevant
contract has automated coverage. `Partial` means useful behavior exists but a
visible path, state, or verification lane is missing. `Missing` is an accepted
Android gap, not an invitation to move a Server responsibility into the app.

## Setup, accounts, and profile

| Frontend behavior | Server contract | Android state | Evidence or next requirement |
| --- | --- | --- | --- |
| Instance discovery and compatibility | `/health`, `/instance` | Implemented | Setup repository and discovery contract tests |
| Local password login | `/auth/login`, `/auth/me`, `/auth/refresh` | Implemented | Scoped authenticator and account validation tests |
| Guest access | `/auth/guest` | Implemented | Capability-driven login screen |
| External-browser OIDC | `/auth/oidc/start`, `/status`, `/callback` | Implemented | OIDC contract, callback parser, encrypted transaction device test |
| Password reset | `/auth/reset-password` | Implemented | Native reset screen and typed errors |
| Registration and first-admin bootstrap | `/auth/register/status`, `/auth/register` | Partial | Native local/OIDC route, bootstrap redirect, Android contract tests, Server route tests, and API 33 Compose tests pass; process recreation and signed-release retest remain |
| Reauthentication of the selected account | `/auth/login`, `/auth/oidc/callback` | Implemented | Account identity is checked before replacing credentials |
| Multiple instances and accounts | Account-scoped authenticated API | Implemented | Room scope, cookies, tokens, workers, cache, and navigation are isolated |
| Profile identity | `/profile`, `/profile/account` | Implemented | Native profile settings and identity Compose test |
| Emoji, custom, and reset avatar | `/profile/avatar/*` | Implemented | Picker, GIF decoder, upload DTO, and repository tests; an authenticated OIDC account exercised WebP and animated GIF uploads, profile refresh, and process recreation on API 29; signed-release retest remains |
| YouTube session status and pairing | `/youtube-session/*` | Partial | Native capability, status, authenticated remote browser, disconnect, expiry, and unavailable states have contract, unit, and API 33 Compose coverage; process recreation and signed-release retest remain |

## Discovery and browsing

| Frontend behavior | Server contract | Android state | Evidence or next requirement |
| --- | --- | --- | --- |
| Home recommendations | `/recommendations/home` | Implemented | Cached section state and Home repository |
| Continue watching with progress | `/progress`, `/history` | Implemented | Native first section and durable progress outbox |
| Trending fallback | `/trending` | Implemented | Home distinguishes fallback from recommendations |
| Subscription feed | `/subscriptions/feed` | Implemented | Progressive client, cached rows, retry status, and load harness |
| Subscribed channel directory | `/subscriptions` | Implemented | Native channels grid and provider tests |
| Channel videos, live, playlists, podcasts | `/channel/page`, `/channel/playlists`, `/podcasts` | Implemented | Continuation contracts and paging tests |
| Search suggestions and history | `/suggestions`, `/search-history` | Implemented | Account-scoped remote history |
| Search services, grouped filters, and pagination | `/search`, `/search/filters` | Implemented | Contract, selection, and continuation tests |
| Public playlist detail and save | `/playlist`, `/saved-playlists` | Implemented | Native detail, pagination, save boundary, and Library cache |
| Podcast episodes and queue | `/podcasts/episodes` | Implemented | Paged detail and service-owned queue |
| Shorts recommendations and subscriptions | `/recommendations/shorts`, `/subscriptions/shorts` | Partial | Native vertical pager, shared Media3 host, autoplay intent, favorite, Watch Later, comments, subscriptions, sharing, blocking, channel interleaving, explicit states, and bounded next-item metadata prefetch have contract, unit, and API 29 Compose coverage. A real beta account exercised both feed endpoints, the empty result, settings round trip, and process recreation; long real playback, seeks, and network transitions remain unverified |
| DeArrow titles and thumbnails | `/dearrow`, `/dearrow/thumbnail` | Implemented | Server-synchronized preferences, trusted-candidate resolution, shared card/player presentation, contract and Compose tests, plus a real beta process-recreation check |
| Block video, channel, and title keyword | `/blocked/*` | Implemented | Native menus/settings and contract tests |
| Notifications and unread count | `/notifications*` | Implemented | Screen, badge, read-all flow, DTO and Compose tests |

## Library, import, and downloads

| Frontend behavior | Server contract | Android state | Evidence or next requirement |
| --- | --- | --- | --- |
| History with paging, search, sort, and clear | `/history` | Implemented | Room Paging, scoped refresh metadata, clear action |
| Favorites and watch later | `/favorites`, `/watch-later` | Implemented | Durable desired-state outbox and cached tabs |
| Playlist create, rename, delete, reorder | `/playlists*` | Implemented | Native dialogs, rollback, and summary/detail separation |
| Saved public playlists | `/saved-playlists` | Implemented | Dedicated cached Library tab |
| TypeType backup export and restore | `/backup/typetype`, `/restore/typetype` | Implemented | Android document picker and multipart contract tests |
| PipePipe restore | `/restore/pipepipe` | Implemented | ZIP validation, summary, refresh, and contract tests |
| YouTube Takeout import | `/imports/youtube-takeout*` | Partial | Native multi-archive queue, long-running scoped work, persisted Server jobs, preview/report states, cache refresh, contract tests, Room tests, and API 33 Compose tests pass; the Server does not advertise this capability and interrupted multipart uploads cannot resume, while real beta, process recreation, and old-API verification remain |
| Server download selection and jobs | `/downloader/jobs*` | Implemented | Native format sheet, WorkManager observation, typed errors |
| Completed artifact transfer | Signed artifact URL | Partial | API 29 and API 34 complete and reconcile a real local DownloadManager transfer; reboot and signed-release retest remain |

## Watch and playback

| Frontend behavior | Server contract | Android state | Evidence or next requirement |
| --- | --- | --- | --- |
| Provider playback | `/streams` and Server media URLs | Implemented | Decoder-aware Media3 source selection |
| YouTube VOD SABR | `/streams/youtube/sabr/bootstrap`, `/sabr/playback/*` | Implemented | Shared lifecycle, bounded windows, recovery, and generation tests |
| Live and DVR playback | Shared SABR live window | Partial | Follower and timeline tests exist; long real live and post-live verification remains |
| Audio-only playback | SABR audio target | Implemented | Service command and audio-only contract/coordinator tests |
| Quality, codec, audio, subtitle, and speed selection | Stream and playback contracts | Implemented | Native sheet state and selection tests |
| Server-proxied YouTube subtitles | `/subtitles/youtube/{videoId}` | Implemented | TTML endpoint mapping, overlay, cue loader, and device tests |
| SponsorBlock actions and chapters | Stream segment metadata and settings | Implemented | Category policy, skip/mute/manual notice, chapter and Compose tests |
| Queue, autoplay, shuffle, and repeat | Stable video identities | Implemented | One service-owned queue and persistence tests |
| Sleep timer | Device-owned Media3 state | Implemented | Service timer and device tests |
| Comments and replies | `/comments`, `/comments/replies` | Implemented | Paging source and native bottom sheet |
| Related videos and channel actions | Stream metadata and subscriptions | Implemented | Shared video menus and player channel state |
| Phone, tablet, fullscreen, mini-player, PiP | MediaSession and device capabilities | Partial | Adaptive watch layout is tested; API 29 and API 34 verify H.264 playback continuing in system PiP and pausing through the same service-owned MediaSession. API 34 also verifies media notification and foreground-service publication with notification permission denied and granted; signed PiP and rotation retest remains |
| Network loss and reconnection | Fresh Server session and bounded retry | Partial | Recovery gates exist; long VOD/live network-transition matrix remains |
| Danmaku overlay | Comment-derived frontend presentation | Missing | Define accessibility, density, performance, and settings behavior first |

## Settings and product states

| Frontend behavior | Android state | Evidence or next requirement |
| --- | --- | --- |
| Appearance, theme, and accent | Implemented | Device-scoped native settings |
| Player defaults and caption style | Implemented | Server settings patches and Android caption integration |
| SponsorBlock preferences | Implemented | Server-synchronized controls and policy tests |
| Content visibility and history privacy | Implemented | Server-synchronized controls and write gates |
| DeArrow preferences | Implemented | Native content controls cover title, thumbnail, and trust modes; a real beta search displayed the Server title and representative frame |
| Diagnostics review, export, and clear | Implemented | Bounded redacted local store and sanitizer tests |
| Native bug report | Implemented | Preview and explicit submission path; pinned Server retest remains |
| Loading, cached refresh, empty, partial, and fatal states | Partial | Major lists preserve cache; semantic state audit is not complete for every route |
| Font scale, TalkBack, RTL, keyboard, D-pad, foldable | Partial | Targeted tests exist; full route matrix is still required |

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
