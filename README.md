# revanced-patches-android5 (patcher API v22)

Bộ patch kiểu ReVanced Extended cho **YouTube 16.40.36** (Android 5+), đã chuyển sang **ReVanced patcher 22.0.0 / API v22** (Gradle 9.3.1, Java 17, plugin `app.revanced.patches` 1.0.0-dev.10).

> Chỉ hỗ trợ đúng APK `com.google.android.youtube` **16.40.36**. Nhiều hook dùng tên đã bị obfuscate của bản này (ví dụ `zkf`, `aaua`, `aiqc`, `yrb`), nên bản khác sẽ không khớp.

## Có gì mới so với bản a5 cũ

| Mục | Nội dung |
|---|---|
| Patcher | Toàn bộ patch viết lại theo API v22 (`bytecodePatch` / `resourcePatch`, fingerprint `composingFirstMethod`, extension `.rve`). |
| **Spoof video streams (A5)** | Bảng client mở rộng để thử mọi khả năng, **17 client, mọi client đều có mặt ở cả 4 danh sách** (client chung, livestream, Shorts, dự phòng): Android VR 1.65 (**đúng giá trị ReVanced gốc: Android 12 / SDK 32**) và biến thể **Android 14 / SDK 34 của rvx**, VR 1.43 (không AV1), VR Pico, Android XR, Android Studio (Creator), Android TV (Unplugged), **Android No SDK** (client mặc định của rvx, có/không đăng nhập), **Android Reel** (có/không đăng nhập), **visionOS** 1.03 / 1.02 (Morphe) và bản **rvx** (OS 26.5.23O471, UA Safari), gửi `osName` = `visionOS` thật sự, và nhóm **TV thử nghiệm** (TV, TV Legacy/Tizen, TV Simply — cần JavaScript/PoToken nên rất có thể không phát). Mặc định bật; client chung = Android VR, **livestream và Shorts = Android No SDK**, dự phòng = Android Studio. Ghi đè header (User-Agent, X-YouTube-Client-*), bỏ thông tin đăng nhập với client không cần đăng nhập. Hai công tắc thử nghiệm: *dùng User-Agent của client cho luồng googlevideo khi xem livestream* (mặc định bật) và *coi Shorts như video thường* (mặc định **bật**: bỏ `params` Shorts, giống ReVanced gốc vốn không bao giờ gửi `params` của app). Mỗi lần phát đều ghi log (tag `revanced`, dòng `Spoof client: ...` và `Fetch livestreams not available: HTTP ...`). Bản trước không có tự tắt spoof: đã gỡ theo yêu cầu. |
| **Spoof app version (A5)** | Có công tắc bật/tắt, mặc định `20.05.46`, tối thiểu `20.02.34` (thấp hơn hoặc để trống sẽ dùng mặc định). Phụ thuộc **Add missing resources**. Không còn cơ chế tự tắt spoof (đã gỡ theo yêu cầu). |
| **Add missing resources** | Bổ sung drawable/icon mà layout mới từ server cần (sửa lỗi app sập khi mở, sai icon chuông/tìm kiếm trên thanh trên cùng, theo hướng của Morphe). **Quy ước của dự án:** mọi việc tích hợp, thêm icon còn thiếu hoặc sửa icon sai đều được gom vào patch này (không tạo patch riêng). Hiện có thêm **2 nút trên thanh trên cùng**: nút *danh sách kênh đăng ký* (chỉ hiện khi đang ở tab **Kênh đăng ký**) và nút *răng cưa cài đặt* (chỉ hiện khi đang ở tab **Bạn**), đặt ngay bên trái nút chuông. Có 2 công tắc trong Cài đặt → Layout → General để tắt từng nút. Nút răng cưa mở cài đặt qua `Shell_SettingsActivity` (activity vỏ chọn tài khoản), nên patch cũng đặt cho activity này giao diện **trong suốt** (`Theme.RvxA5.TransparentShell`) để không nháy cửa sổ đen có tên app trước khi vào cài đặt. |
| **Disable layout updates / Freeze layout updates** | Port từ rvx a6, sửa theo Morphe, viết lại thành một "guard" dùng chung: khi **Spoof app version** đang bật (hoặc bật *Disable layout updates*) app **bỏ qua hoàn toàn layout đã lưu** và dùng layout gốc 16.40.36 như một lần cài mới, nên layout của server 20.x không bao giờ được nạp. Config nhận được lúc đang spoof bị đánh dấu "nhiễm": nếu sau đó tắt spoof, lần mở đầu vẫn bỏ qua nó một lần để server gửi config đúng phiên bản thật. *Freeze* chỉ chụp config khi không bị nhiễm. Ở lần chạy đầu khi đang spoof, hộp thoại khuyên bật chế độ máy bay ~10 giây và nút Khởi động lại đếm ngược 10 giây. |
| **Disable update screen (A5)** | Giữ hook a5 trên 16.40.36 (hướng a6 không áp dụng được cho bản này). |
| **Sửa crash mỗi lần mở app** | Trong 16.40.36, hàm khởi tạo cấu hình client-event (`actm.<init>`) bỏ dở giữa chừng khi gặp loại sự kiện lạ của config mới từ server, để trường `g` = null và app văng NullPointerException. Patch layout guard nay bỏ qua mục lạ và chạy tiếp vòng lặp. |
| **Đổi tên patch có hậu tố `(A5)`** | 11 patch trùng tên với bundle ReVanced chính thức nay có hậu tố `(A5)` (Disable haptic feedback, Disable update screen, GmsCore support, Hide info cards, Hide shorts components, Return YouTube Dislike, SponsorBlock, Spoof app version, Spoof video streams, Swipe controls, Theme) để không bị chạy lẫn khi nạp cùng bundle gốc (đã gây hỏng trang cài đặt ReVanced). |
| Ads | Thêm tên component quảng cáo/shopping mới từ rv6 và Morphe. |

## Build bằng GitHub Actions

1. Tạo repo trên GitHub và push toàn bộ thư mục này (đã có `.github/workflows/`).
2. Vào tab **Actions → Release → Run workflow** (hoặc push lên nhánh `main` / `dev`).
3. Khi chạy xong, tải artifact **revanced-patches-output**: bên trong có file `patches-*.rvp`.

Plugin Gradle của ReVanced nằm trên **GitHub Packages**, nên workflow cần `GITHUB_TOKEN` (đã khai báo sẵn bằng `ORG_GRADLE_PROJECT_githubPackagesUsername/Password`). Các link `source`/`website`/`contact` của patch tự lấy từ biến `GITHUB_REPOSITORY`.

### Build trên máy

```bash
export ORG_GRADLE_PROJECT_githubPackagesUsername=<tên GitHub>
export ORG_GRADLE_PROJECT_githubPackagesPassword=<token có quyền read:packages>
./gradlew :patches:buildAndroid
# kết quả: patches/build/libs/patches-*.rvp
```

Cần JDK 17. Dùng file `.rvp` với ReVanced CLI / Manager **hỗ trợ patcher 22** (bản cũ chỉ đọc API cũ sẽ không nạp được).

> **Chỉ nạp MỘT bundle khi vá YouTube 16.40.36.** Hai bundle cùng nạp trong một lần chạy CLI dùng chung một class loader: các patch trùng tên (Settings, Spoof video streams, SponsorBlock, Return YouTube Dislike, ...) sẽ chạy cả hai bản và bản của bundle kia (nhắm vào bản YouTube mới hơn) sẽ báo lỗi. Extension của bundle này có tên riêng `extensions/youtube-rvxa5.rve` để không bị lẫn với `extensions/youtube.rve` của ReVanced chính thức, nhưng vẫn nên dùng đúng một `-p patches-*.rvp` kèm `--exclusive`:
>
> `java -jar revanced-cli.jar patch -p patches-*.rvp --exclusive -e "Settings" -e "Hide general ads" youtube-16.40.36.apk`
>
> **Tên gói (package name):** patch **GmsCore support (A5)** của bundle này đặt tên gói đúng bằng giá trị option bạn nhập (ví dụ `app.vinh2201.android5.youtube`), không tự thêm hậu tố. Nếu app cài ra có thêm đuôi `.revanced`, đó là do một patch khác (thường là **"Change package name"** của ReVanced chính thức, mô tả: *Appends ".revanced" to the package name by default*) đã chạy cùng. Hãy bỏ chọn patch đó, hoặc dùng `--exclusive` như trên.

## Bộ patch nên chọn để chạy được YouTube hiện nay

1. **Settings**, **GmsCore support (A5)** (nếu không root), **Translations** (tuỳ chọn).
2. **Spoof video streams (A5)**: giúp xem video khi client mặc định bị chặn.
3. **Spoof app version (A5)** (kéo theo **Add missing resources**): server không còn phục vụ client quá cũ.
4. **Disable layout updates** hoặc **Freeze layout updates**: bật một trong hai để có công tắc; guard layout cũng tự chạy khi **Spoof app version (A5)** được vá (xem bảng trên). **Lưu ý:** vì layout đã lưu bị bỏ qua khi spoof, vài tính năng phụ thuộc layout "mới" của server (container nút, zoom to fill ở lần chạy đầu) có thể kém hơn bản không spoof.
5. **Disable update screen (A5)**.

Trong Cài đặt của app (ReVanced Extended → Misc) có thể đổi client, đổi phiên bản spoof, bật/tắt từng chức năng. Phiên bản spoof gợi ý: `20.05.46` (khuyên dùng), `20.13.41` (khôi phục action bar không thu gọn), `20.20.42` (khôi phục bộ lọc components cũ), `21.11.48` (khôi phục hành vi phụ đề cũ).

## Giới hạn đã biết (đọc trước khi dùng)

- **Chưa chạy Gradle build và chưa thử trên thiết bị thật.** Mọi kiểm tra đều là tĩnh: lint, kiểm tra cú pháp Kotlin, đối chiếu fingerprint với smali 16.40.36, biên dịch thử phần extension Java bằng stub. Lần build đầu tiên trên GitHub có thể báo lỗi biên dịch nhỏ cần sửa.
- Các client spoof (VR / XR / Studio / TV / No SDK / Reel / visionOS) **có thể ngừng hoạt động bất cứ lúc nào** khi YouTube đổi phía server. Studio và TV cần đăng nhập; VR/XR không phát được video giới hạn độ tuổi, trẻ em, trả phí; Studio không phát được livestream.
- **Livestream báo HTTP 400, Shorts với Android VR không tải, Android TV đôi khi không chạy**: nguyên nhân CHƯA được xác minh (chưa có môi trường chạy thật). Giả thuyết: (a) yêu cầu luồng googlevideo dùng User-Agent của app trong khi URL nhúng `c=<client>`, (b) tham số `params` của Shorts làm client không phải app Android từ chối. Đã thêm 2 công tắc thử nghiệm và log để kiểm chứng; xem mục *Thử nghiệm client* bên dưới.
- **Xoá dữ liệu app** (hoặc cài lại) sau khi đổi bản patch, vì cấu hình layout và cài đặt cũ có thể còn sót.
- Nếu repo của bạn còn thư mục `extensions/youtube` (thừa, từ bản cũ), hãy **xoá** nó: bundle này chỉ dùng `extensions/youtube-rvxa5`.
- **Client cố ý KHÔNG đưa vào** (đã đối chiếu rv22 / rvx / Morphe): iOS, TestSuite, Embedded (rv cũ, không còn trả luồng dùng được); Web và Mobile Web (cần PoToken + JavaScript, chắc chắn 403); Android Music No SDK / Music Reel (Morphe ghi: *không phát được video nào trong app YouTube*); Android VR / XR **1.73.21** của Morphe (chỉ trả luồng SABR, app 16.40.36 không phát được; thay bằng bản 1.64.34 / 1.69.27); TV Embedded (cần màn hình EMBED mà hook này không đặt được); `ANDROID` gốc (chính là tắt spoof).
- Các hook ghi đè header (`awa.o`), bảng ánh xạ icon, và cơ chế layout updates chưa được kiểm chứng khi chạy.
- `protobufspoof` chưa được port (patch cũ đã vô hiệu); khối `ENABLE_PROTOBUF_SPOOF` trong `revanced_prefs.xml` chỉ là chú thích không có tác dụng.
- Một vài fingerprint (màu seekbar, landscape...) viết cứng tên obfuscated của 16.40.36.
- Chỉ so sánh sâu với rv6/Morphe ở các phần liên quan đến spoof, layout updates, VP9, layout switch và bộ lọc quảng cáo. Các bộ lọc litho khác, hằng số GmsCore, danh sách màu theme chưa được cập nhật theo rv6/Morphe.
- Bản dịch mới (chuỗi thêm trong bản này) chỉ có tiếng Anh.
- **2 nút trên thanh trên cùng (Add missing resources)** chỉ được kiểm tra tĩnh (fingerprint khớp đúng 1 chỗ: `owc.pu`, `owc.p`, `fpm.l`; biên dịch thử phần Java). Các giả định chưa chạy thật: (a) id tab của thanh dưới có chứa `subscriptions` / `library`; (b) trang con (kênh, kết quả tìm kiếm...) làm nút biến mất nhờ `owc.p`; mỗi tab có thanh trên/menu riêng nên nút được cập nhật trên **mọi** menu đã dựng (bản đầu chỉ nhớ 1 menu cuối nên quay lại tab thì nút mất); (c) nút nằm tận cùng bên trái của cụm nút hành động, nên nếu nút Cast đang hiện thì nút mới có thể nằm bên trái Cast chứ không sát chuông; (d) nút kênh đăng ký mở `https://m.youtube.com/feed/channels` qua chính app (chưa xác minh đường dẫn này còn được `Shell_UrlActivity` xử lý). Nếu hoạt động sai, gửi các dòng logcat có tag `revanced` (mình có ghi log "Pivot tab selected" / "Page is the root of a tab").

## Thử nghiệm client

1. Chỉ nạp bundle này (`--exclusive`), cài lại hoặc xoá dữ liệu app.
2. Settings → ReVanced Extended → Misc → Spoof video streams: chọn client ở từng danh sách (chung / livestream / Shorts / dự phòng); tất cả 12 client đều có trong cả 4 danh sách.
3. Livestream: thử lần lượt *Android No SDK*, *Android No SDK (đăng nhập)*, *Android VR*, *visionOS*; thử bật/tắt công tắc User-Agent livestream.
4. Shorts: thử *Android VR* (đúng cấu hình ReVanced gốc), *Android No SDK*, *Android Reel*; công tắc *coi Shorts như video thường* đã bật sẵn, nếu Shorts lỗi thì thử tắt nó.
5. Nếu vẫn lỗi, gửi lại log `adb logcat -s revanced` các dòng `Spoof client: ...` và `Fetch livestreams not available: HTTP ...` để xác định đúng nguyên nhân.

## Cấu trúc

```
patches/            patch Kotlin + tài nguyên (patches/src/main/resources/youtube/...)
extensions/youtube-rvxa5/ mã Java chạy trong app (integrations)
docs/PORTING_GUIDE.md  ghi chú chuyển đổi API cũ -> v22
.github/workflows/  build_pull_request.yml, release.yml
```

## Danh sách patch

### `youtube/ads`

| Patch | Mặc định | Mô tả |
|---|---|---|
| Hide general ads | bật | Hooks the method which parses the bytes into a ComponentContext to filter components. |
| Hide video ads | bật | Removes ads in the video player. |

### `youtube/button`

| Patch | Mặc định | Mô tả |
|---|---|---|
| Overlay buttons | bật | Add overlay buttons for ReVanced Extended. |

### `youtube/extended`

| Patch | Mặc định | Mô tả |
|---|---|---|
| Force VP9 codec | bật | Forces the VP9 codec for videos. |
| Layout switch | bật | Tricks the dpi to use some tablet/phone layouts. |
| Spoof app version (A5) | bật | Spoofs the YouTube client version (default 20.05.46, minimum 20.02.34) so the server keeps serving the app. Needs "Add missing resources" because the server then answers with the layout of the new version. |

### `youtube/layout`

| Patch | Mặc định | Mô tả |
|---|---|---|
| Hide comment component | bật | Adds options to hide comment component under a video. |
| Enable old quality layout | bật | Enables the original quality flyout menu. |
| Hide endscreen overlay | bật | Hide endscreen overlay on swipe controls. |
| Hide fullscreen panels | bật | Hides video description and comments panel in fullscreen view. |
| Disable haptic feedback (A5) | bật | Disable haptic feedback when swiping. |
| Disable landscape mode | tắt | Disable landscape mode when entering fullscreen. |
| Hide auto captions | bật | Hide captions from being automatically enabled. |
| Hide auto player popup panels | bật | Hide automatic popup panels (playlist or live chat) on video player. |
| Hide crowdfunding box | bật | Hides the crowdfunding box between the player and video description. |
| Header switch | bật | Add switch to change header. |
| Hide mix playlists | bật | Removes mix playlists from home feed and video player. |
| Hide email address | bật | Hides the email address in the account switcher. |
| Hide create button | bật | Hides the create button in the navigation bar. |
| Hide shorts button | bật | Hides the shorts button in the navigation bar. |
| Switch create notification | bật | Switches the create button and notification button. |
| Hide shorts components (A5) | bật | Hides other Shorts components. |
| Hide snackbar | bật | Hides the snackbar action popup. |
| Enable tablet mini player | bật | Enables the tablet mini player layout. |
| Enable wide search bar | bật | Replaces the search icon with a wide search bar. This will hide the YouTube logo when active. |
| Hide autoplay button | bật | Hides the autoplay button in the video player. |
| Hide player captions button | bật | Hides the captions button in the video player. |
| Hide cast button | bật | Hides the cast button in the video player. |
| Hide endscreen cards | bật | Hides the suggested video cards at the end of a video in fullscreen. |
| Hide info cards (A5) | bật | Hides info-cards in videos. |
| Hide live chat button | bật | Hides the live chat button in the video player. |
| Hide next prev button | bật | Hides the next prev button in the player controller. |
| Hide player overlay filter | bật | Remove the dark filter layer from the player's background. |
| Hide suggested actions | bật | Hide the suggested actions bar inside the player. |
| Hide channel watermark | bật | Hides creator's watermarks on videos. |
| Enable seekbar tapping | bật | Enables tap-to-seek on the seekbar of the video player. |
| Hide time and seekbar | bật | Hides progress bar and time counter on videos. |

### `youtube/misc`

| Patch | Mặc định | Mô tả |
|---|---|---|
| Custom branding icon Afn (Blue) | tắt | Changes the YouTube launcher icon (Afn / Blue). |
| Custom branding icon Afn (Red) | bật | Changes the YouTube launcher icon (Afn / Red). |
| Custom branding icon Revancify | tắt | Changes the YouTube launcher icon (Revancify). |
| Custom branding name | bật | Changes the YouTube launcher name to your choice (defaults to ReVanced Extended). |
| Enable external browser | bật | Use an external browser to open the url. |
| Force premium heading | tắt | Forces premium heading on the home screen. |
| Disable layout updates | bật | Adds an option to disable layout updates by the server. |
| Freeze layout updates | bật | Adds an option to keep the layout configuration of the first start and ignore later layout updates. |
| Material You | tắt | Enables MaterialYou theme for Android 12+ |
| GmsCore support (A5) | bật | Allows YouTube ReVanced to run without root and under a different package name with GmsCore. ("GmsCore" is new Vanced MicroG). |
| Enable minimized playback | bật | Enables minimized and background playback. |
| Add missing resources | bật | Adds drawables and icon handling that newer server responses expect, which prevents crashes and wrong icons (bell, search, create) when the client version is spoofed. Also adds the buttons the old UI is missing: the list of subscribed channels (Subscriptions tab) and the settings gear (You tab) in the top bar. Anything that integrates or adds missing/corrects wrong icons goes into this patch. |
| Enable open links directly | bật | Bypass URL redirects (youtube.com/redirect) when opening links in video descriptions. |
| Optimize resource | bật | Removes duplicate resources and adds missing translation files from YouTube. |
| Check android version | bật | If the device is Android 6.0 or higher, show a warning dialog. |
| Hide first-time background notification | bật | Disable notification when you launch background play for the first time. |
| Return YouTube Dislike (A5) | bật | Shows the dislike count of videos using the Return YouTube Dislike API. |
| Settings | bật | Applies mandatory patches to implement ReVanced settings into the application. |
| SponsorBlock (A5) | bật | Integrates SponsorBlock which allows skipping video segments such as sponsored content. |
| Spoof video streams (A5) | bật | Spoofs the client of the player request (Android VR / Android No SDK / visionOS / Android Studio / ...) so that videos can be played when the default client is blocked or unsupported. |
| Theme (A5) | bật | Applies a custom theme (default: amoled). |
| Hide tooltip content | bật | Hides the tooltip box that appears on first install. |
| Translations | bật | Add Crowdin Translations. |
| Disable update screen (A5) | bật | Disables the app update screen. |

### `youtube/swipe`

| Patch | Mặc định | Mô tả |
|---|---|---|
| Swipe controls (A5) | bật | Adds volume and brightness swipe controls. |

### `youtube/video`

| Patch | Mặc định | Mô tả |
|---|---|---|
| Custom video speed | bật | Adds more video speed options. |
| Default video quality | bật | Adds ability to set default video quality settings. |
| Default video speed | bật | Adds ability to set default video speed settings. |

## Giấy phép

GNU General Public License v3.0 (xem `LICENSE`). Dựa trên mã của ReVanced, ReVanced Extended (inotia00 / kitadai31 / d4n3436) và Morphe.
