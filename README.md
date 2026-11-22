# revanced-patches-android5 (patcher API v22)

Bộ patch kiểu ReVanced Extended cho **YouTube 16.40.36** (Android 5+), đã chuyển sang **ReVanced patcher 22.0.0 / API v22** (Gradle 9.3.1, Java 17, plugin `app.revanced.patches` 1.0.0-dev.10).

> Chỉ hỗ trợ đúng APK `com.google.android.youtube` **16.40.36**. Nhiều hook dùng tên đã bị obfuscate của bản này (ví dụ `zkf`, `aaua`, `aiqc`, `yrb`), nên bản khác sẽ không khớp.

## Có gì mới so với bản a5 cũ

| Mục | Nội dung |
|---|---|
| Patcher | Toàn bộ patch viết lại theo API v22 (`bytecodePatch` / `resourcePatch`, fingerprint `composingFirstMethod`, extension `.rve`). |
| **Spoof video streams (A5)** | Bảng client mở rộng để thử mọi khả năng, **17 client, mọi client đều có mặt ở cả 4 danh sách** (client chung, livestream, Shorts, dự phòng): Android VR 1.65 (**đúng giá trị ReVanced gốc: Android 12 / SDK 32**) và biến thể **Android 14 / SDK 34 của rvx**, VR 1.43 (không AV1), VR Pico, Android XR, Android Studio (Creator), Android TV (Unplugged, app YouTube TV **10.46.1**), **Android No SDK** (client mặc định của rvx, có/không đăng nhập), **Android Reel** (có/không đăng nhập), **visionOS** 1.03 / 1.02 và bản **rvx** (OS 26.5.23O471, UA Safari), gửi `osName` = `visionOS` thật sự, và nhóm **TV thử nghiệm** (TV, TV Legacy/Tizen, TV Simply — cần JavaScript/PoToken nên rất có thể không phát). Mặc định bật; client chung = Android VR, **livestream và Shorts = Android No SDK**, dự phòng = Android Studio. Ghi đè header (User-Agent, X-YouTube-Client-*), bỏ thông tin đăng nhập với client không cần đăng nhập. Hai công tắc thử nghiệm: *dùng User-Agent của client cho luồng googlevideo khi xem livestream* (mặc định bật) và *coi Shorts như video thường* (mặc định **bật**: bỏ `params` Shorts, giống ReVanced gốc vốn không bao giờ gửi `params` của app). Mỗi lần phát đều ghi log (tag `revanced`, dòng `Spoof client: ...` và `Fetch livestreams not available: HTTP ...`). Bản trước không có tự tắt spoof: đã gỡ theo yêu cầu. **Mặc định (cả code lẫn màn Cài đặt):** chung = Android VR, livestream = **Android No SDK**, Shorts = Android No SDK, video không phát được (dự phòng) = **Android No SDK** (Android Studio/Creator không phát được livestream nên không còn là mặc định). Nếu máy đã từng mở Cài đặt ở bản cũ thì giá trị cũ (Android VR) đã được lưu sẵn và phải đổi tay hoặc xoá dữ liệu app. |
| **Spoof app version (A5)** | Có công tắc bật/tắt, mặc định `20.05.46`, tối thiểu `20.02.34` (thấp hơn hoặc để trống sẽ dùng mặc định). Phụ thuộc **Add missing resources (A5)**. Không còn cơ chế tự tắt spoof (đã gỡ theo yêu cầu). |
| **Add missing resources (A5)** | Bổ sung drawable/icon mà layout mới từ server cần (sửa lỗi app sập khi mở, sai icon chuông/tìm kiếm trên thanh trên cùng). **Quy ước của dự án:** mọi việc tích hợp, thêm icon còn thiếu hoặc sửa icon sai đều được gom vào patch này (không tạo patch riêng). Hiện có thêm **2 nút trên thanh trên cùng**: nút *danh sách kênh đăng ký* (chỉ hiện khi đang ở tab **Kênh đăng ký**) và nút *răng cưa cài đặt* (chỉ hiện khi đang ở tab **Bạn**), đặt ngay bên trái nút chuông. Có 2 công tắc trong Cài đặt → Layout → General để tắt từng nút. Hai nút chỉ hiện ở **trang gốc** của tab (trang không có mũi tên quay lại), nên khi bấm chuông sang trang Thông báo hay vào kênh/tìm kiếm thì nút tự biến mất và quay lại khi về trang gốc. Nút răng cưa **gọi thẳng mục "Cài đặt" của chính app** (cùng hàm mà mục Cài đặt trong menu 3 chấm của trang Thông báo dùng, nên không có cửa sổ trung gian). App chỉ tạo mục đó sau khi đã mở một trang có menu 3 chấm chuẩn (ví dụ Thông báo), nên ngay sau khi mở app, lần bấm đầu có thể đi đường dự phòng qua `Shell_SettingsActivity` (activity vỏ chọn tài khoản); activity này được đặt giao diện **trong suốt, không thanh tiêu đề** (`Theme.RvxA5.TransparentShell`, kế thừa `Theme.YouTube.Dark.NoActionBar`) để không hiện cửa sổ đen có tên app. **Sửa popup 3 chấm của trình phát (không có công tắc tắt vì đây là phần bắt buộc):** khi spoof lên 20.x, server gửi thêm các dòng *Chất lượng / Tốc độ phát / Phụ đề / Bản âm thanh / Chế độ cài đặt khác* theo kiểu 20.x; lệnh đi kèm là của bản mới mà app 16.40.36 không biết nên bấm vào chỉ đóng popup, và app còn tự thêm các dòng gốc của nó bên dưới nên popup hiện mỗi mục hai lần (một nhóm chết, một nhóm chạy). Cách sửa: popup (cả 2 biến thể trong app) dựng danh sách từ 2 nguồn — dòng do server gửi, rồi dòng gốc của app; patch bỏ nguồn thứ nhất ngay sau khi nó được đọc (đường code xử lý "không có dòng server" vốn có sẵn trong app), nên **chỉ còn một danh sách duy nhất: các dòng gốc của app, theo đúng ngôn ngữ của bản APK đang cài** (không dịch thêm, không so khớp nhãn/lệnh của server nên không lệ thuộc spoof). Mọi dòng gốc đều bấm được và mở đúng bảng chọn. Các mục của *Chế độ cài đặt khác* trong 20.x (Lặp video, Xem ở chế độ thực tế ảo, Trợ giúp và phản hồi, Thống kê chi tiết) đều đã là dòng gốc trong danh sách; *Chế độ môi trường xung quanh* và *Âm lượng ổn định* không tồn tại trong 16.40.36 nên không có. Lưu ý: nội dung bảng **Chất lượng** (cần từ 2 mức trở lên) và **Phụ đề / Bản âm thanh** do phản hồi `/player` của client đang spoof cung cấp; client nào không trả về dữ liệu đó thì dòng vẫn hiện nhưng bảng rỗng hoặc ghi *Unavailable* — menu không tự tạo ra dữ liệu. |
| **Disable layout updates (A5) / Freeze layout updates (A5)** | Port từ rvx a6, viết lại thành một "guard" dùng chung: khi **Spoof app version** đang bật (hoặc bật *Disable layout updates (A5)*) app **bỏ qua hoàn toàn layout đã lưu** và dùng layout gốc 16.40.36 như một lần cài mới, nên layout của server 20.x không bao giờ được nạp. Config nhận được lúc đang spoof bị đánh dấu "nhiễm": nếu sau đó tắt spoof, lần mở đầu vẫn bỏ qua nó một lần để server gửi config đúng phiên bản thật. *Freeze* chỉ chụp config khi không bị nhiễm. Ở lần chạy đầu khi đang spoof, hộp thoại khuyên bật chế độ máy bay ~10 giây và nút Khởi động lại đếm ngược 10 giây.  **Guard không bao giờ tự bật/tắt các công tắc *Disable/Freeze layout updates (A5)*** (bộ phát hiện "app sập trong 6 giây thì tự bật" từng có đã được gỡ hẳn); chỉ bác tự gạt trong Cài đặt. **Ngoại lệ duy nhất (theo yêu cầu):** khi bác **tắt *Spoof app version* trong Cài đặt**, màn Cài đặt tự bật *cả hai* công tắc *Disable layout updates* và *Freeze layout updates* **một lần** (để layout đã lưu từ lúc spoof không làm app thật bị sập); sau đó bác tự tắt lại được, không có gì tự bật lại, và lúc khởi động app cũng không tự bật gì. |
| **Disable update screen (A5)** | Giữ hook a5 trên 16.40.36 (hướng a6 không áp dụng được cho bản này). |
| **Sửa crash mỗi lần mở app** | Trong 16.40.36, hàm khởi tạo cấu hình client-event (`actm.<init>`) bỏ dở giữa chừng khi gặp loại sự kiện lạ của config mới từ server, để trường `g` = null và app văng NullPointerException. Patch layout guard nay bỏ qua mục lạ và chạy tiếp vòng lặp. |
| **Đổi tên patch có hậu tố `(A5)`** | **Toàn bộ 65 patch có tên** đều có hậu tố `(A5)` để không bị chạy lẫn hay chọn nhầm khi nạp cùng bundle khác. 11 patch đổi trước (Disable haptic feedback, Disable update screen, GmsCore support, Hide info cards, Hide shorts components, Return YouTube Dislike, SponsorBlock, Spoof app version, Spoof video streams, Swipe controls, Theme) từng gây hỏng trang cài đặt ReVanced khi trùng tên; 54 patch còn lại (Settings, Translations, Add missing resources, Hide general ads, ...) đổi tiếp theo yêu cầu. **Quy ước:** patch mới thêm vào cũng phải có hậu tố `(A5)`. Chỉ đổi *tên patch*; tên công tắc/mục trong Cài đặt của app giữ nguyên. Lệnh CLI cũ dùng `-e "tên cũ"` cần sửa sang tên mới. |
| Ads | Thêm tên component quảng cáo/shopping mới từ rv6 và cộng đồng. |

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

> **Chỉ nạp MỘT bundle khi vá YouTube 16.40.36.** Hai bundle cùng nạp trong một lần chạy CLI dùng chung một class loader: trước đây các patch trùng tên (Settings, Spoof video streams, SponsorBlock, Return YouTube Dislike, ...) chạy cả hai bản và bản của bundle kia (nhắm vào bản YouTube mới hơn) báo lỗi. **Từ nay mọi patch của bundle này đều có hậu tố `(A5)`** nên `-e "tên"` không còn chọn nhầm patch cùng tên của bundle khác. Extension của bundle này có tên riêng `extensions/youtube-rvxa5.rve` để không bị lẫn với `extensions/youtube.rve` của ReVanced chính thức, nhưng vẫn nên dùng đúng một `-p patches-*.rvp` kèm `--exclusive`:
>
> `java -jar revanced-cli.jar patch -p patches-*.rvp --exclusive -e "Settings (A5)" -e "Hide general ads (A5)" youtube-16.40.36.apk`
>
> **Tên gói (package name):** patch **GmsCore support (A5)** của bundle này đặt tên gói đúng bằng giá trị option bạn nhập (ví dụ `app.vinh2201.android5.youtube`), không tự thêm hậu tố. Nếu app cài ra có thêm đuôi `.revanced`, đó là do một patch khác (thường là **"Change package name"** của ReVanced chính thức, mô tả: *Appends ".revanced" to the package name by default*) đã chạy cùng. Hãy bỏ chọn patch đó, hoặc dùng `--exclusive` như trên.

## Bộ patch nên chọn để chạy được YouTube hiện nay

1. **Settings (A5)**, **GmsCore support (A5)** (nếu không root), **Translations (A5)** (tuỳ chọn).
2. **Spoof video streams (A5)**: giúp xem video khi client mặc định bị chặn.
3. **Spoof app version (A5)** (kéo theo **Add missing resources (A5)**): server không còn phục vụ client quá cũ.
4. **Disable layout updates (A5)** hoặc **Freeze layout updates (A5)**: bật một trong hai để có công tắc; guard layout cũng tự chạy khi **Spoof app version (A5)** được vá (xem bảng trên). **Lưu ý:** vì layout đã lưu bị bỏ qua khi spoof, vài tính năng phụ thuộc layout "mới" của server (container nút, zoom to fill ở lần chạy đầu) có thể kém hơn bản không spoof.
5. **Disable update screen (A5)**.

Trong Cài đặt của app (ReVanced Extended → Misc) có thể đổi client, đổi phiên bản spoof, bật/tắt từng chức năng. Phiên bản spoof gợi ý: `20.05.46` (khuyên dùng), `20.13.41` (khôi phục action bar không thu gọn), `20.20.42` (khôi phục bộ lọc components cũ), `21.11.48` (khôi phục hành vi phụ đề cũ).

## Giới hạn đã biết (đọc trước khi dùng)

- **Chưa chạy Gradle build và chưa thử trên thiết bị thật.** Mọi kiểm tra đều là tĩnh: lint, kiểm tra cú pháp Kotlin, đối chiếu fingerprint với smali 16.40.36, biên dịch thử phần extension Java bằng stub. Lần build đầu tiên trên GitHub có thể báo lỗi biên dịch nhỏ cần sửa.
- Các client spoof (VR / XR / Studio / TV / No SDK / Reel / visionOS) **có thể ngừng hoạt động bất cứ lúc nào** khi YouTube đổi phía server. Studio và TV cần đăng nhập; VR/XR không phát được video giới hạn độ tuổi, trẻ em, trả phí; Studio không phát được livestream.
- **Livestream báo HTTP 400, Shorts với Android VR không tải, Android TV đôi khi không chạy**: nguyên nhân CHƯA được xác minh (chưa có môi trường chạy thật). Giả thuyết: (a) yêu cầu luồng googlevideo dùng User-Agent của app trong khi URL nhúng `c=<client>`, (b) tham số `params` của Shorts làm client không phải app Android từ chối. Đã thêm 2 công tắc thử nghiệm và log để kiểm chứng; xem mục *Thử nghiệm client* bên dưới.
- **Xoá dữ liệu app** (hoặc cài lại) sau khi đổi bản patch, vì cấu hình layout và cài đặt cũ có thể còn sót.
- Nếu repo của bạn còn thư mục `extensions/youtube` (thừa, từ bản cũ), hãy **xoá** nó: bundle này chỉ dùng `extensions/youtube-rvxa5`.
- **Client cố ý KHÔNG đưa vào** (đã đối chiếu rv22 / rvx): iOS, TestSuite, Embedded (rv cũ, không còn trả luồng dùng được); Web và Mobile Web (cần PoToken + JavaScript, chắc chắn 403); Android Music No SDK / Music Reel (cộng đồng ghi: *không phát được video nào trong app YouTube*); Android VR / XR **1.73.21** của cộng đồng (chỉ trả luồng SABR, app 16.40.36 không phát được; thay bằng bản 1.64.34 / 1.69.27); TV Embedded (cần màn hình EMBED mà hook này không đặt được); `ANDROID` gốc (chính là tắt spoof).
- Các hook ghi đè header (`awa.o`), bảng ánh xạ icon, và cơ chế layout updates chưa được kiểm chứng khi chạy.
- `protobufspoof` chưa được port (patch cũ đã vô hiệu); khối `ENABLE_PROTOBUF_SPOOF` trong `revanced_prefs.xml` chỉ là chú thích không có tác dụng.
- Một vài fingerprint (màu seekbar, landscape...) viết cứng tên obfuscated của 16.40.36.
- Chỉ so sánh sâu với rv6 và cộng đồng ở các phần liên quan đến spoof, layout updates, VP9, layout switch và bộ lọc quảng cáo. Các bộ lọc litho khác, hằng số GmsCore, danh sách màu theme chưa được cập nhật theo rv6 và cộng đồng.
- Bản dịch mới (chuỗi thêm trong bản này) chỉ có tiếng Anh.
- **2 nút trên thanh trên cùng (Add missing resources (A5))** chỉ được kiểm tra tĩnh (fingerprint khớp đúng 1 chỗ: `owc.pu`, `owc.p`, `fpm.l`; biên dịch thử phần Java). Các giả định chưa chạy thật: (a) id tab của thanh dưới có chứa `subscriptions` / `library`; (b) trang con (kênh, kết quả tìm kiếm...) làm nút biến mất nhờ `owc.p`; mỗi tab có thanh trên/menu riêng nên nút được cập nhật trên **mọi** menu đã dựng (bản đầu chỉ nhớ 1 menu cuối nên quay lại tab thì nút mất); (c) nút nằm tận cùng bên trái của cụm nút hành động, nên nếu nút Cast đang hiện thì nút mới có thể nằm bên trái Cast chứ không sát chuông; (d) nút kênh đăng ký mở `https://m.youtube.com/feed/channels` qua chính app (chưa xác minh đường dẫn này còn được `Shell_UrlActivity` xử lý). Nếu hoạt động sai, gửi các dòng logcat có tag `revanced` (mình có ghi log "Pivot tab selected" / "Page is the root of a tab").
- **Popup 3 chấm của trình phát (Add missing resources (A5))** chỉ được kiểm tra tĩnh (2 fingerprint `onItemClick` chỉ khớp đúng 1 lớp mỗi biến thể: `nwi`, `nwl`; trong mỗi lớp chỉ có đúng 1 hàm dựng adapter đọc cả 2 danh sách; biên dịch thử). Giả định chưa chạy thật: sau khi bỏ danh sách dòng server, popup chỉ còn các dòng gốc tiếng Anh của APK (APK hiện chỉ có tiếng Anh nên không có bản dịch). Bảng Chất lượng/Phụ đề/Bản âm thanh phụ thuộc dữ liệu `/player` của từng client spoof (một số client không trả phụ đề hoặc chỉ có 1 mức chất lượng mà app dùng được, ví dụ Android VR). Để chẩn đoán vì sao một client báo *Unavailable*, mở đúng video đó bằng client cần kiểm tra rồi gửi logcat các dòng `revanced: FormatSelectionLog` (patch *Spoof video streams (A5)* ghi 2 dòng khi đang spoof: toàn bộ format app nhận được + danh sách còn lại cho bảng chất lượng; chỉ đọc, không đổi cách phát), cùng dòng `revanced: LiveStreamRendererRequester ... Playability check` (kết quả bước kiểm tra phát được không, xem bên dưới).
- **Bước kiểm tra "phát được không" (Spoof video streams (A5))**: trước mỗi video thường, patch gửi một yêu cầu `/player` nhỏ bằng client chung (không đăng nhập) để biết video có phát được và có phải livestream không. Nếu bước này trả về "không phát được", video chuyển sang client của mục *Unplayable video* (mặc định Android No SDK, chỉ có 1 mức 360p nên bảng Chất lượng báo Unavailable). Log thực tế cho thấy mọi video đều bị coi là "unplayable" nên client chung (ví dụ Android VR) không hề được dùng. Bản này: gửi kèm `X-Goog-Visitor-Id` của chính app, thử thêm biến thể VR thứ hai trước khi kết luận, và ghi lý do vào log (`Playability check: ... status=... / reason`). Nếu video vẫn bị chuyển sang client dự phòng, đặt *Unplayable video* = client chung để luôn dùng client đó.
- **Client Android VR (và các client không phải Android thường)**: so với bản ReVanced chính thức, yêu cầu `/player` của bản này khác ở các điểm sau, đã chỉnh cho giống: (1) app gốc ký mọi yêu cầu bằng header `X-Goog-YTSpatula` (chữ ký "tôi là app YouTube"); với client khác Android thường (VR, Creator, TV...) header này mâu thuẫn với client khai báo nên bị bỏ, bản chính thức cũng không gửi header nào của app ngoài `X-Goog-Visitor-Id`/`Authorization` (chỉ khi cần); (2) User-Agent kiểu thiết bị (VR, Creator, Reel...) không còn đuôi ` gzip`, đúng định dạng của bản chính thức; (3) khi phản hồi chỉ có **một** mức VP9 nhưng có **nhiều** mức H.264, các định dạng VP9 bị bỏ để app dùng cả bậc H.264 (bản chính thức làm đúng việc này) — nhờ đó mục *Chất lượng* không còn bị rút về một dòng "Unavailable" (log: `AvcQualityLadder ... Quality ladder: VP9 left out`). Ngoài ra log `Player request headers: [...]` liệt kê TÊN các header của yêu cầu `/player` (không ghi giá trị) để biết app đang gửi những gì. Các thay đổi này chưa chạy thật (chỉ kiểm tra tĩnh); nếu Android VR vẫn không phát, gửi logcat các dòng `Spoof client`, `Playability check`, `Player request headers` của một video.
- **Android VR: lỗi HTTP 400 và "Sign in to confirm you're not a bot"** (từ logcat thật): (1) yêu cầu `/player` của app là body protobuf ký như app YouTube (khóa + chữ ký trong URL, header Spatula, mọi header của app); với họ client Android VR (VR, VR Pico, VR A14, VR 1.43, XR) máy chủ trả `HTTP 400 Request contains an invalid argument` (app hiện *There was a problem with the server [400]*). Với các client này yêu cầu được gửi lại đúng kiểu bản chính thức: URL `.../youtubei/v1/player?alt=proto` (phản hồi vẫn là protobuf mà app đọc được), body JSON chỉ có thông tin client + `videoId` + `contentCheckOk`/`racyCheckOk`, header chỉ gồm `Content-Type`, `User-Agent`, `X-YouTube-Client-Name/Version` cùng `X-Goog-Visitor-Id` và `X-GOOG-API-FORMAT-VERSION` của app (log: `Player request sent like the official patches`). Chỉ áp dụng cho `/youtubei/v1/player` và chỉ khi `videoId` có trong body gốc, các client khác giữ nguyên. (2) Bước kiểm tra trước (`Playability check`) nhận *LOGIN_REQUIRED – not a bot* khi chưa có visitor id (video đầu tiên sau khi mở app): câu trả lời này không còn bị tính là "unplayable"; bước kiểm tra tự lấy `responseContext.visitorData` do máy chủ cấp rồi hỏi lại một lần (log: `asking again with the visitor id of the server`, `visitorIdSent=`).

## Thử nghiệm client

1. Chỉ nạp bundle này (`--exclusive`), cài lại hoặc xoá dữ liệu app.
2. Settings → ReVanced Extended → Misc → Spoof video streams: chọn client ở từng danh sách (chung / livestream / Shorts / dự phòng); tất cả 17 client đều có trong cả 4 danh sách.
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
| Hide general ads (A5) | bật | Hooks the method which parses the bytes into a ComponentContext to filter components. |
| Hide video ads (A5) | bật | Removes ads in the video player. |

### `youtube/button`

| Patch | Mặc định | Mô tả |
|---|---|---|
| Overlay buttons (A5) | bật | Add overlay buttons for ReVanced Extended. |

### `youtube/extended`

| Patch | Mặc định | Mô tả |
|---|---|---|
| Force VP9 codec (A5) | bật | Forces the VP9 codec for videos. |
| Layout switch (A5) | bật | Tricks the dpi to use some tablet/phone layouts. |
| Spoof app version (A5) | bật | Spoofs the YouTube client version (default 20.05.46, minimum 20.02.34) so the server keeps serving the app. Needs "Add missing resources" because the server then answers with the layout of the new version. |

### `youtube/layout`

| Patch | Mặc định | Mô tả |
|---|---|---|
| Hide comment component (A5) | bật | Adds options to hide comment component under a video. |
| Enable old quality layout (A5) | bật | Enables the original quality flyout menu. |
| Hide endscreen overlay (A5) | bật | Hide endscreen overlay on swipe controls. |
| Hide fullscreen panels (A5) | bật | Hides video description and comments panel in fullscreen view. |
| Disable haptic feedback (A5) | bật | Disable haptic feedback when swiping. |
| Disable landscape mode (A5) | tắt | Disable landscape mode when entering fullscreen. |
| Hide auto captions (A5) | bật | Hide captions from being automatically enabled. |
| Hide auto player popup panels (A5) | bật | Hide automatic popup panels (playlist or live chat) on video player. |
| Hide crowdfunding box (A5) | bật | Hides the crowdfunding box between the player and video description. |
| Header switch (A5) | bật | Add switch to change header. |
| Hide mix playlists (A5) | bật | Removes mix playlists from home feed and video player. |
| Hide email address (A5) | bật | Hides the email address in the account switcher. |
| Hide create button (A5) | bật | Hides the create button in the navigation bar. |
| Hide shorts button (A5) | bật | Hides the shorts button in the navigation bar. |
| Switch create notification (A5) | bật | Switches the create button and notification button. |
| Hide shorts components (A5) | bật | Hides other Shorts components. |
| Hide snackbar (A5) | bật | Hides the snackbar action popup. |
| Enable tablet mini player (A5) | bật | Enables the tablet mini player layout. |
| Enable wide search bar (A5) | bật | Replaces the search icon with a wide search bar. This will hide the YouTube logo when active. |
| Hide autoplay button (A5) | bật | Hides the autoplay button in the video player. |
| Hide player captions button (A5) | bật | Hides the captions button in the video player. |
| Hide cast button (A5) | bật | Hides the cast button in the video player. |
| Hide endscreen cards (A5) | bật | Hides the suggested video cards at the end of a video in fullscreen. |
| Hide info cards (A5) | bật | Hides info-cards in videos. |
| Hide live chat button (A5) | bật | Hides the live chat button in the video player. |
| Hide next prev button (A5) | bật | Hides the next prev button in the player controller. |
| Hide player overlay filter (A5) | bật | Remove the dark filter layer from the player's background. |
| Hide suggested actions (A5) | bật | Hide the suggested actions bar inside the player. |
| Hide channel watermark (A5) | bật | Hides creator's watermarks on videos. |
| Enable seekbar tapping (A5) | bật | Enables tap-to-seek on the seekbar of the video player. |
| Hide time and seekbar (A5) | bật | Hides progress bar and time counter on videos. |

### `youtube/misc`

| Patch | Mặc định | Mô tả |
|---|---|---|
| Custom branding icon Afn (Blue) (A5) | tắt | Changes the YouTube launcher icon (Afn / Blue). |
| Custom branding icon Afn (Red) (A5) | bật | Changes the YouTube launcher icon (Afn / Red). |
| Custom branding icon Revancify (A5) | tắt | Changes the YouTube launcher icon (Revancify). |
| Custom branding name (A5) | bật | Changes the YouTube launcher name to your choice (defaults to ReVanced Extended). |
| Enable external browser (A5) | bật | Use an external browser to open the url. |
| Force premium heading (A5) | tắt | Forces premium heading on the home screen. |
| Disable layout updates (A5) | bật | Adds an option to disable layout updates by the server. |
| Freeze layout updates (A5) | bật | Adds an option to keep the layout configuration of the first start and ignore later layout updates. |
| Material You (A5) | tắt | Enables MaterialYou theme for Android 12+ |
| GmsCore support (A5) | bật | Allows YouTube ReVanced to run without root and under a different package name with GmsCore. ("GmsCore" is new Vanced MicroG). |
| Enable minimized playback (A5) | bật | Enables minimized and background playback. |
| Add missing resources (A5) | bật | Adds drawables and icon handling that newer server responses expect, which prevents crashes and wrong icons (bell, search, create) when the client version is spoofed. Also adds the buttons the old UI is missing: the list of subscribed channels (Subscriptions tab) and the settings gear (You tab) in the top bar. Anything that integrates or adds missing/corrects wrong icons goes into this patch. Also fixes the player three-dots menu when the client is spoofed to 20.x: the server sends its own settings rows (quality, speed, captions, audio track, additional settings) whose commands this old version does not know (a tap only closed the menu), and the app adds its own native rows below them, so every entry showed twice. The menu (both variants) now ignores the server rows and shows one list only: the native rows of the app, in the language of the installed APK, all of them working (no option to turn it off, nothing depends on the server labels). What the quality, captions and audio track choosers contain still depends on the `/player` response of the spoofed client. |
| Enable open links directly (A5) | bật | Bypass URL redirects (youtube.com/redirect) when opening links in video descriptions. |
| Optimize resource (A5) | bật | Removes duplicate resources and adds missing translation files from YouTube. |
| Check android version (A5) | bật | If the device is Android 6.0 or higher, show a warning dialog. |
| Hide first-time background notification (A5) | bật | Disable notification when you launch background play for the first time. |
| Return YouTube Dislike (A5) | bật | Shows the dislike count of videos using the Return YouTube Dislike API. |
| Settings (A5) | bật | Applies mandatory patches to implement ReVanced settings into the application. |
| SponsorBlock (A5) | bật | Integrates SponsorBlock which allows skipping video segments such as sponsored content. |
| Spoof video streams (A5) | bật | Spoofs the client of the player request (Android VR / Android No SDK / visionOS / Android Studio / ...) so that videos can be played when the default client is blocked or unsupported. |
| Theme (A5) | bật | Applies a custom theme (default: amoled). |
| Hide tooltip content (A5) | bật | Hides the tooltip box that appears on first install. |
| Translations (A5) | bật | Add Crowdin Translations. |
| Disable update screen (A5) | bật | Disables the app update screen. |

### `youtube/swipe`

| Patch | Mặc định | Mô tả |
|---|---|---|
| Swipe controls (A5) | bật | Adds volume and brightness swipe controls. |

### `youtube/video`

| Patch | Mặc định | Mô tả |
|---|---|---|
| Custom video speed (A5) | bật | Adds more video speed options. |
| Default video quality (A5) | bật | Adds ability to set default video quality settings. |
| Default video speed (A5) | bật | Adds ability to set default video speed settings. |

## Giấy phép

GNU General Public License v3.0 (xem `LICENSE`). Dựa trên mã của ReVanced, ReVanced Extended (inotia00 / kitadai31 / d4n3436) và các dự án mã nguồn mở khác trong cộng đồng.
