# revanced-patches-android5 (patcher API v22)

Bộ patch kiểu ReVanced Extended cho **YouTube 16.40.36** (Android 5+), đã chuyển sang **ReVanced patcher 22.0.0 / API v22** (Gradle 9.3.1, Java 17, plugin `app.revanced.patches` 1.0.0-dev.10).

> Chỉ hỗ trợ đúng APK `com.google.android.youtube` **16.40.36**. Nhiều hook dùng tên đã bị obfuscate của bản này (ví dụ `zkf`, `aaua`, `aiqc`, `yrb`), nên bản khác sẽ không khớp.

## Có gì mới so với bản a5 cũ

| Mục | Nội dung |
|---|---|
| Patcher | Toàn bộ patch viết lại theo API v22 (`bytecodePatch` / `resourcePatch`, fingerprint `composingFirstMethod`, extension `.rve`). |
| **Spoof video streams** | Thay patch spoof client cũ. Bảng client mới (chỉ client Android còn trả luồng trực tiếp): Android VR 1.65, VR 1.43 (không AV1), VR Pico, Android XR, Android Studio (Creator), Android TV (Unplugged). Đã bỏ iOS, TestSuite, Android, Embedded, Web, TV simply embedded. Mặc định bật, client chung/Shorts = Android VR, dự phòng = Android Studio. Có ghi đè header (User-Agent, X-YouTube-Client-*) và bỏ thông tin đăng nhập với client không cần đăng nhập. |
| **Spoof app version** | Có công tắc bật/tắt, mặc định `20.05.46`, tối thiểu `20.02.34` (thấp hơn hoặc để trống sẽ dùng mặc định). Có cơ chế tự tắt spoof nếu app không mở được 2 lần liên tiếp. Phụ thuộc **Add missing resources**. |
| **Add missing resources** | Bổ sung drawable/icon mà layout mới từ server cần (sửa lỗi app sập khi mở, sai icon chuông/tìm kiếm trên thanh trên cùng, theo hướng của Morphe). |
| **Disable layout updates / Freeze layout updates** | Port từ rvx a6, sửa theo Morphe, viết lại thành một "guard" dùng chung: khi **Spoof app version** đang bật (hoặc bật *Disable layout updates*) app **bỏ qua hoàn toàn layout đã lưu** và dùng layout gốc 16.40.36 như một lần cài mới, nên layout của server 20.x không bao giờ được nạp. Config nhận được lúc đang spoof bị đánh dấu "nhiễm": nếu sau đó tắt spoof, lần mở đầu vẫn bỏ qua nó một lần để server gửi config đúng phiên bản thật. *Freeze* chỉ chụp config khi không bị nhiễm. Ở lần chạy đầu khi đang spoof, hộp thoại khuyên bật chế độ máy bay ~10 giây và nút Khởi động lại đếm ngược 10 giây. |
| **Disable update screen** | Giữ hook a5 trên 16.40.36 (hướng a6 không áp dụng được cho bản này). |
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
> **Tên gói (package name):** patch `MicroG support` của bundle này đặt tên gói đúng bằng giá trị option bạn nhập (ví dụ `app.vinh2201.android5.youtube`), không tự thêm hậu tố. Nếu app cài ra có thêm đuôi `.revanced`, đó là do một patch khác (thường là **"Change package name"** của ReVanced chính thức, mô tả: *Appends ".revanced" to the package name by default*) đã chạy cùng. Hãy bỏ chọn patch đó, hoặc dùng `--exclusive` như trên.

## Bộ patch nên chọn để chạy được YouTube hiện nay

1. **Settings**, **GmsCore support** (nếu không root), **Translations** (tuỳ chọn).
2. **Spoof video streams**: giúp xem video khi client mặc định bị chặn.
3. **Spoof app version** (kéo theo **Add missing resources**): server không còn phục vụ client quá cũ.
4. **Disable layout updates** hoặc **Freeze layout updates**: bật một trong hai để có công tắc; guard layout cũng tự chạy khi **Spoof app version** được vá (xem bảng trên). **Lưu ý:** vì layout đã lưu bị bỏ qua khi spoof, vài tính năng phụ thuộc layout "mới" của server (container nút, zoom to fill ở lần chạy đầu) có thể kém hơn bản không spoof.
5. **Disable update screen**.

Trong Cài đặt của app (ReVanced Extended → Misc) có thể đổi client, đổi phiên bản spoof, bật/tắt từng chức năng. Phiên bản spoof gợi ý: `20.05.46` (khuyên dùng), `20.13.41` (khôi phục action bar không thu gọn), `20.20.42` (khôi phục bộ lọc components cũ), `21.11.48` (khôi phục hành vi phụ đề cũ).

## Giới hạn đã biết (đọc trước khi dùng)

- **Chưa chạy Gradle build và chưa thử trên thiết bị thật.** Mọi kiểm tra đều là tĩnh: lint, kiểm tra cú pháp Kotlin, đối chiếu fingerprint với smali 16.40.36, biên dịch thử phần extension Java bằng stub. Lần build đầu tiên trên GitHub có thể báo lỗi biên dịch nhỏ cần sửa.
- Các client spoof (VR / XR / Studio / TV) **có thể ngừng hoạt động bất cứ lúc nào** khi YouTube đổi phía server. Studio và TV cần đăng nhập; VR/XR không phát được video giới hạn độ tuổi, trẻ em, trả phí.
- iOS và các client không phải Android được bỏ có chủ đích (không còn trả luồng dùng được cho app cũ này).
- Các hook ghi đè header (`awa.o`), bảng ánh xạ icon, và cơ chế layout updates chưa được kiểm chứng khi chạy.
- `protobufspoof` chưa được port (patch cũ đã vô hiệu); khối `ENABLE_PROTOBUF_SPOOF` trong `revanced_prefs.xml` chỉ là chú thích không có tác dụng.
- Một vài fingerprint (màu seekbar, landscape...) viết cứng tên obfuscated của 16.40.36.
- Chỉ so sánh sâu với rv6/Morphe ở các phần liên quan đến spoof, layout updates, VP9, layout switch và bộ lọc quảng cáo. Các bộ lọc litho khác, hằng số GmsCore, danh sách màu theme chưa được cập nhật theo rv6/Morphe.
- Bản dịch mới (chuỗi thêm trong bản này) chỉ có tiếng Anh.

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
| Spoof app version | bật | Spoofs the YouTube client version (default 20.05.46, minimum 20.02.34) so the server keeps serving the app. Needs "Add missing resources" because the server then answers with the layout of the new version. |

### `youtube/layout`

| Patch | Mặc định | Mô tả |
|---|---|---|
| Hide comment component | bật | Adds options to hide comment component under a video. |
| Enable old quality layout | bật | Enables the original quality flyout menu. |
| Hide endscreen overlay | bật | Hide endscreen overlay on swipe controls. |
| Hide fullscreen panels | bật | Hides video description and comments panel in fullscreen view. |
| Disable haptic feedback | bật | Disable haptic feedback when swiping. |
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
| Hide shorts components | bật | Hides other Shorts components. |
| Hide snackbar | bật | Hides the snackbar action popup. |
| Enable tablet mini player | bật | Enables the tablet mini player layout. |
| Enable wide search bar | bật | Replaces the search icon with a wide search bar. This will hide the YouTube logo when active. |
| Hide autoplay button | bật | Hides the autoplay button in the video player. |
| Hide player captions button | bật | Hides the captions button in the video player. |
| Hide cast button | bật | Hides the cast button in the video player. |
| Hide endscreen cards | bật | Hides the suggested video cards at the end of a video in fullscreen. |
| Hide info cards | bật | Hides info-cards in videos. |
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
| GmsCore support | bật | Allows YouTube ReVanced to run without root and under a different package name with GmsCore. ("GmsCore" is new Vanced MicroG). |
| Enable minimized playback | bật | Enables minimized and background playback. |
| Add missing resources | bật | Adds drawables and icon handling that newer server responses expect, which prevents crashes and wrong icons (bell, search, create) when the client version is spoofed. |
| Enable open links directly | bật | Bypass URL redirects (youtube.com/redirect) when opening links in video descriptions. |
| Optimize resource | bật | Removes duplicate resources and adds missing translation files from YouTube. |
| Check android version | bật | If the device is Android 6.0 or higher, show a warning dialog. |
| Hide first-time background notification | bật | Disable notification when you launch background play for the first time. |
| Return YouTube Dislike | bật | Shows the dislike count of videos using the Return YouTube Dislike API. |
| Settings | bật | Applies mandatory patches to implement ReVanced settings into the application. |
| SponsorBlock | bật | Integrates SponsorBlock which allows skipping video segments such as sponsored content. |
| Spoof video streams | bật | Spoofs the client of the player request (Android VR / Android Studio / ...) so that videos can be played when the default client is blocked or unsupported. |
| Theme | bật | Applies a custom theme (default: amoled). |
| Hide tooltip content | bật | Hides the tooltip box that appears on first install. |
| Translations | bật | Add Crowdin Translations. |
| Disable update screen | bật | Disables the app update screen. |

### `youtube/swipe`

| Patch | Mặc định | Mô tả |
|---|---|---|
| Swipe controls | bật | Adds volume and brightness swipe controls. |

### `youtube/video`

| Patch | Mặc định | Mô tả |
|---|---|---|
| Custom video speed | bật | Adds more video speed options. |
| Default video quality | bật | Adds ability to set default video quality settings. |
| Default video speed | bật | Adds ability to set default video speed settings. |

## Giấy phép

GNU General Public License v3.0 (xem `LICENSE`). Dựa trên mã của ReVanced, ReVanced Extended (inotia00 / kitadai31 / d4n3436) và Morphe.
