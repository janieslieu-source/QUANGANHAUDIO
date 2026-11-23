# Ghi chú dự án – App Quản lý bảo hành Quang Anh

File này để Claude (hoặc người lập trình) mở phiên mới đọc là nắm được app và cách làm việc.

## Cách làm việc với chủ app (bắt buộc)
- Chủ app không rành kỹ thuật: **trả lời bằng tiếng Việt đơn giản**, tránh từ chuyên môn.
- **Mỗi lần sửa phải chạy thử (Playwright) và gửi hình chụp cho từng bước / hành động.**
- Sửa xong: commit lên nhánh làm việc → tạo PR → **tự gộp (merge) vào `main`** (chủ app đã cho phép "Từ giờ tự gộp") → báo link PR.
- Không force push. Không ghi tên model AI vào commit / PR.
- **Mỗi lần sửa `index.html` phải tăng `window.APP_BUILD`** (đầu thẻ `<script>`), để điện thoại tự tải bản mới.
- **Không đưa dữ liệu khách hàng, file Excel của khách, mật khẩu, file khóa ký APK (release.jks) lên GitHub** – kho này công khai.

## App là gì
- Một file duy nhất: `index.html` (HTML + CSS + JavaScript thuần, gói trong 1 hàm tự chạy).
- Chạy trên GitHub Pages: https://janieslieu-source.github.io/QUANGANHAUDIO/
- Dữ liệu: Firebase Firestore (đăng nhập ẩn danh); mất mạng thì tạm lưu trên máy (localStorage).
- Bản cài Android: thư mục `android/` (WebView mở link trên), GitHub Actions `.github/workflows/android-apk.yml` tự đóng gói APK, file tải ở `tai-app/`.
- Logo / icon: `brand/`, `logo.png`, `icon-*.png`, `manifest.webmanifest`.

## Bộ phận (role) và tab
`bh` Bảo hành sửa chữa · `gd` Giám đốc · `kt` Kỹ thuật · `vc` Vận chuyển · `ac` Kế toán · `sa` Bán hàng · `wh` Kho · tab `cc` Chấm công, `nc` Ngày công, `pm` Phần mềm (chỉ Giám đốc).
Mỗi tab có mật mã riêng (lưu trong app, không ghi ở đây).

## Dữ liệu Firestore (collection)
`tk` phiếu bảo hành/sửa chữa · `od` đơn hàng · `st` hàng có seri trong kho · `mv` biến động tồn kho (nhập/xuất) · `ex` chi phí + nhiều loại bản ghi khác (theo `kind`) · `reg` sổ bảo hành theo seri · `ri` nhập sổ bảo hành từ file · `ph` ảnh.
- `mv.kind`: nhap, xuat, kiemke, linhkien (KT thay cho khách), lk_test (KT mượn test), lk_test_tra (trả lại), doimoi, lammoi, muon…
- `ex.kind`: nhap (nhập hàng NCC – cũng là nợ NCC), tranoncc (trả nợ NCC), ship, dodung, luong, khac, và các loại KHÔNG tính chi phí: nhanvien, bangluong, chamcong, cclog, cc_cfg, muon, fw, fw_cfg, swsale, kkreq, lkmuon, lmreq, upreq, ctreq.

## Quy trình chính
- **Phiếu sửa chữa / bảo hành:** Bảo hành nhận → Giám đốc duyệt (nếu "liên hệ khách sau" thì GĐ điền lỗi) → Kho nhận hàng → Kỹ thuật kiểm tra lỗi, xử lý → Vận chuyển gửi trả → Kế toán thu tiền / công nợ.
- **Phụ kiện kèm theo máy:** lúc nhận phiếu tick "Có phụ kiện" + ghi tên (tk.accessories) → hiện trên thẻ / chi tiết phiếu, bảng Kỹ thuật; Vận chuyển gửi trả bắt buộc tick "Đã gửi kèm phụ kiện".
- **Đổi hàng bảo hành:** Giám đốc duyệt → Kho xuất → Vận chuyển.
- **Đơn hàng (Bán hàng):** Bán hàng tạo đơn → Kho xuất (seri) → Vận chuyển gửi → Kế toán.
  - Có thu cũ đổi mới (nhắc sau 5 ngày nếu khách chưa gửi hàng cũ), phần mềm, hàng tặng.
  - Dòng **🚚 Giao thẳng từ nhà cung cấp**: không qua kho; có ô giá nhập (không bắt buộc) → tự ghi nợ NCC; chưa có giá thì mục Chi phí nhắc, 17h chuông nhắc Kế toán/GĐ. Đơn toàn hàng giao thẳng → chuyển thẳng Kế toán.
- **Box Android – mã kích hoạt:** Kho xuất kho dòng box Android (isBox) có ô mã kích hoạt theo thứ tự seri → ghi reg.actCode + line.actCodes → tự hiện ở tab 💿 Phần mềm (pmLicenses) và tra cứu bảo hành.
- **Vận chuyển gửi chành xe:** phí ship (bắt buộc) + phí gửi hàng (không bắt buộc), mỗi phí chọn Công ty trả / Khách trả; tổng ghi vào Chi phí – Ship hàng.
- **Kế toán ghi thanh toán:** ô Tiền hàng + phí chành xe đã tick = Tổng công nợ, trừ tạm ứng = còn nợ.
- **Linh kiện mượn test (tab Kỹ thuật):** chọn Test sản phẩm mới nhập về / Test cho khách (chọn phiếu) → tìm trong kho → gửi Kho đồng ý mới trừ kho → hạn trả 3 ngày, quá hạn nhắc (có Gia hạn 3 ngày) → Kho bấm Nhận lại cộng kho. Không có trong kho → ô Giải pháp chờ Kho duyệt. Khi Hoàn tất phiếu: từng linh kiện chọn Thay cho khách / Trả về kho.
- **Kỹ thuật – Đổi đầu thu / đầu phát** (bộ HDMI không dây…): phương án xử lý `doidau` → chọn bộ phận, seri cũ (trong bộ của khách), seri mới (bắt buộc) → sổ bảo hành (`reg`) thay seri cũ bằng seri mới, giữ hạn BH, ghi `swaps`; seri mới có trong kho thì xuất (`outKind: doidau`). Phiếu lưu `partSwap`.
- **Kho:** xuất kho theo loại (Xuất làm mới sản phẩm, KT thay cho khách, Đổi cho khách, KT mượn test, Xuất bán theo đơn), tổng hợp + Excel; trả tiền NCC → Kho duyệt nhập hàng; sửa số tồn cần GĐ duyệt; kiểm kê theo file.
- **Kỹ thuật – Xuất làm mới sản phẩm:** chọn hàng trong kho → gửi Kho duyệt xuất (ex kind `lmreq`) → Kho duyệt mới trừ kho (mv `lammoi`).
- **Kỹ thuật – Tạm xuất đi công trình:** chọn hàng trong kho + số lượng, tên công trình, khách, SĐT, địa chỉ → Kho duyệt xuất (ex kind `ctreq`, mv `congtrinh`) → sau đó Trả về kho (mv `congtrinh_tra`) / Chốt đã lắp theo từng món; hết hàng ở công trình thì tự chốt.
- **Kỹ thuật – Nâng cấp sản phẩm:** sản phẩm cũ + linh kiện → trừ kho ngay (mv `nangcap`) → Kho duyệt nhập sản phẩm mới (mv `nangcap_nhap`, tạo sản phẩm nếu chưa có); từ chối thì hoàn kho (`nangcap_huy`). Bản ghi ex kind `upreq`.
- **Kho – Bổ sung seri (mục Tồn kho):** hàng tồn theo số lượng → ghi seri (gõ / quét / ảnh) → tạo bản ghi `st` + mv `gan_seri` âm cùng số lượng, tổng tồn không đổi.
- **Kế toán – Nhập hàng NCC:** nhiều dòng (tối đa 10) có số lượng, đơn giá, VAT; mỗi dòng 1 bản ghi `nhap` chờ Kho duyệt nhập.
- **Giám đốc – Báo cáo cuối ngày:** tồn kho + doanh thu ngày, tải Excel.
- **Số seri chỉ ở Kho / Bán hàng / Bảo hành** (nhập kho, xuất kho, bán, bảo hành) – KHÔNG hiện trong Chi phí / Nhập hàng NCC của Kế toán. Tồn kho: mỗi dòng tick Có seri / Không seri, sản phẩm có seri hiện "Seri x/tồn" và nút Bổ sung seri khi thiếu.
- **Quy định seri (SN_RULE):** 525 + 5 số (hàng công ty) HOẶC seri hãng Tomko bắt đầu 900 / 1845 (6–20 ký tự). Đọc ảnh / mã vạch cũng nhận seri Tomko.
- **Thông báo:** tất cả gom vào nút chuông đỏ góc dưới phải, bấm mới hiện; ẩn khi đang gõ.
- **Tự cập nhật:** app kiểm tra `APP_BUILD` trên máy chủ mỗi phút / khi mở lại → có bản mới thì tự tải lại.

## Tìm kiếm sản phẩm
Ưu tiên Model → Hãng → Tên; Kỹ thuật ưu tiên linh kiện (danh sách `LK_PRIORITY`, `LK_WORDS`). Dùng hàm `attachPicker`, `rankProducts`, chỉ mục `dataIdx` cho nhanh.

## Chạy thử
- Mở `file:///…/index.html` bằng Playwright (Chromium có sẵn), giả lập điện thoại (`devices['Pixel 7']` / `iPhone 13`).
- Đặt sẵn dữ liệu qua `localStorage` (`baohanh_tk`, `baohanh_od`, `baohanh_mv`, `baohanh_ex`…), chọn bộ phận bằng `sessionStorage.bh_role`, đặt `localStorage.bh_act`.
- Chặn mạng Firebase (`gstatic`, `jsdelivr`) để app chạy chế độ lưu trên máy; thư viện Excel (xlsx) cần cấp file tại chỗ nếu mạng chặn cdnjs.
- Kiểm tra cú pháp: tách phần `<script>` ra file rồi `node --check`.
