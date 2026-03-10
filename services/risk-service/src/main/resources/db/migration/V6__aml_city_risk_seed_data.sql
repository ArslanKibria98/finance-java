-- V6__aml_city_risk_seed_data.sql
-- Saudi Arabian Cities Risk Classification for AML Scoring
-- Based on EastNets AML Risk Schema + SAMA AML/CFT Guidelines
--
-- Risk Classification Criteria:
--   HIGH:   Border cities near conflict zones (Yemen, Iraq), known smuggling corridors,
--           maritime borders, high informal economy activity
--   MEDIUM: Major commercial/financial hubs, pilgrimage cities, industrial zones,
--           military cities near borders, port cities
--   LOW:    Interior residential cities, agricultural towns, smaller administrative centers
--
-- Region Codes (Saudi Administrative Regions / Emirates):
--   01 = Riyadh, 02 = Makkah, 03 = Madinah, 04 = Eastern Province,
--   05 = Qassim, 06 = Asir, 07 = Tabuk, 08 = Ha'il,
--   09 = Northern Borders, 10 = Jazan, 11 = Najran, 12 = Al Bahah, 13 = Al Jawf

DO $$
DECLARE
    seed_tenant UUID := '00000000-0000-0000-0000-000000000001';
BEGIN

-- =====================================================================
-- Region 01: Riyadh Region (العاصمة الرياض)
-- =====================================================================
INSERT INTO aml_city_risk_scores (tenant_id, city_id, region_id, name_ar, name_en, risk_level)
VALUES
    (seed_tenant, '01-001', '01', 'الرياض', 'Riyadh', 'MEDIUM'),
    (seed_tenant, '01-002', '01', 'الخرج', 'Al Kharj', 'LOW'),
    (seed_tenant, '01-003', '01', 'الدرعية', 'Ad Diriyah', 'LOW'),
    (seed_tenant, '01-004', '01', 'المجمعة', 'Al Majmaah', 'LOW'),
    (seed_tenant, '01-005', '01', 'وادي الدواسر', 'Wadi ad-Dawasir', 'LOW'),
    (seed_tenant, '01-006', '01', 'الأفلاج', 'Al Aflaj', 'LOW'),
    (seed_tenant, '01-007', '01', 'الدوادمي', 'Ad Dawadmi', 'LOW'),
    (seed_tenant, '01-008', '01', 'شقراء', 'Shaqra', 'LOW'),
    (seed_tenant, '01-009', '01', 'الزلفي', 'Az Zulfi', 'LOW'),
    (seed_tenant, '01-010', '01', 'حوطة بني تميم', 'Hotat Bani Tamim', 'LOW'),
    (seed_tenant, '01-011', '01', 'المزاحمية', 'Al Muzahmiyya', 'LOW'),
    (seed_tenant, '01-012', '01', 'رماح', 'Rumah', 'LOW'),
    (seed_tenant, '01-013', '01', 'ثادق', 'Thadiq', 'LOW'),
    (seed_tenant, '01-014', '01', 'الحريق', 'Al Hariq', 'LOW'),
    (seed_tenant, '01-015', '01', 'ليلى', 'Layla', 'LOW'),
    (seed_tenant, '01-016', '01', 'السليل', 'As Sulayyil', 'LOW'),
    (seed_tenant, '01-017', '01', 'عفيف', 'Afif', 'LOW'),
    (seed_tenant, '01-018', '01', 'الغاط', 'Al Ghat', 'LOW'),
    (seed_tenant, '01-019', '01', 'حريملاء', 'Huraymila', 'LOW'),
    (seed_tenant, '01-020', '01', 'الدلم', 'Ad Dilam', 'LOW');

-- =====================================================================
-- Region 02: Makkah Region (منطقة مكة المكرمة)
-- =====================================================================
INSERT INTO aml_city_risk_scores (tenant_id, city_id, region_id, name_ar, name_en, risk_level)
VALUES
    (seed_tenant, '02-001', '02', 'مكة المكرمة', 'Makkah', 'MEDIUM'),
    (seed_tenant, '02-002', '02', 'جدة', 'Jeddah', 'MEDIUM'),
    (seed_tenant, '02-003', '02', 'الطائف', 'Taif', 'MEDIUM'),
    (seed_tenant, '02-004', '02', 'القنفذة', 'Al Qunfudhah', 'MEDIUM'),
    (seed_tenant, '02-005', '02', 'رابغ', 'Rabigh', 'MEDIUM'),
    (seed_tenant, '02-006', '02', 'الليث', 'Al Lith', 'LOW'),
    (seed_tenant, '02-007', '02', 'تربة', 'Turbah', 'LOW'),
    (seed_tenant, '02-008', '02', 'رنية', 'Ranyah', 'LOW'),
    (seed_tenant, '02-009', '02', 'الكامل', 'Al Kamil', 'LOW'),
    (seed_tenant, '02-010', '02', 'خليص', 'Khulays', 'LOW'),
    (seed_tenant, '02-011', '02', 'الجموم', 'Al Jumum', 'LOW'),
    (seed_tenant, '02-012', '02', 'المويه', 'Al Muwayh', 'LOW');

-- =====================================================================
-- Region 03: Madinah Region (منطقة المدينة المنورة)
-- =====================================================================
INSERT INTO aml_city_risk_scores (tenant_id, city_id, region_id, name_ar, name_en, risk_level)
VALUES
    (seed_tenant, '03-001', '03', 'المدينة المنورة', 'Madinah', 'MEDIUM'),
    (seed_tenant, '03-002', '03', 'ينبع', 'Yanbu', 'MEDIUM'),
    (seed_tenant, '03-003', '03', 'العلا', 'Al Ula', 'LOW'),
    (seed_tenant, '03-004', '03', 'بدر', 'Badr', 'LOW'),
    (seed_tenant, '03-005', '03', 'خيبر', 'Khaybar', 'LOW'),
    (seed_tenant, '03-006', '03', 'المهد', 'Al Mahd', 'LOW'),
    (seed_tenant, '03-007', '03', 'الحناكية', 'Al Hanakiyah', 'LOW'),
    (seed_tenant, '03-008', '03', 'أملج', 'Umluj', 'LOW');

-- =====================================================================
-- Region 04: Eastern Province (المنطقة الشرقية)
-- =====================================================================
INSERT INTO aml_city_risk_scores (tenant_id, city_id, region_id, name_ar, name_en, risk_level)
VALUES
    (seed_tenant, '04-001', '04', 'الدمام', 'Dammam', 'MEDIUM'),
    (seed_tenant, '04-002', '04', 'الخبر', 'Al Khobar', 'MEDIUM'),
    (seed_tenant, '04-003', '04', 'الظهران', 'Dhahran', 'MEDIUM'),
    (seed_tenant, '04-004', '04', 'الجبيل', 'Al Jubail', 'MEDIUM'),
    (seed_tenant, '04-005', '04', 'الأحساء', 'Al Ahsa', 'MEDIUM'),
    (seed_tenant, '04-006', '04', 'الهفوف', 'Al Hofuf', 'MEDIUM'),
    (seed_tenant, '04-007', '04', 'القطيف', 'Al Qatif', 'MEDIUM'),
    (seed_tenant, '04-008', '04', 'حفر الباطن', 'Hafar Al-Batin', 'HIGH'),
    (seed_tenant, '04-009', '04', 'الخفجي', 'Al Khafji', 'HIGH'),
    (seed_tenant, '04-010', '04', 'رأس تنورة', 'Ras Tanura', 'MEDIUM'),
    (seed_tenant, '04-011', '04', 'بقيق', 'Buqayq', 'LOW'),
    (seed_tenant, '04-012', '04', 'النعيرية', 'An Nuayriyah', 'LOW'),
    (seed_tenant, '04-013', '04', 'قرية العليا', 'Qaryat Al Ulya', 'LOW'),
    (seed_tenant, '04-014', '04', 'الجفر', 'Al Jafr', 'LOW');

-- =====================================================================
-- Region 05: Qassim Region (منطقة القصيم)
-- =====================================================================
INSERT INTO aml_city_risk_scores (tenant_id, city_id, region_id, name_ar, name_en, risk_level)
VALUES
    (seed_tenant, '05-001', '05', 'بريدة', 'Buraidah', 'LOW'),
    (seed_tenant, '05-002', '05', 'عنيزة', 'Unaizah', 'LOW'),
    (seed_tenant, '05-003', '05', 'الرس', 'Ar Rass', 'LOW'),
    (seed_tenant, '05-004', '05', 'المذنب', 'Al Mithnab', 'LOW'),
    (seed_tenant, '05-005', '05', 'البكيرية', 'Al Bukayriyah', 'LOW'),
    (seed_tenant, '05-006', '05', 'البدائع', 'Al Badai', 'LOW'),
    (seed_tenant, '05-007', '05', 'رياض الخبراء', 'Riyadh Al Khabra', 'LOW'),
    (seed_tenant, '05-008', '05', 'عيون الجواء', 'Uyun Al Jiwa', 'LOW');

-- =====================================================================
-- Region 06: Asir Region (منطقة عسير)
-- Near Yemen border — elevated risk for border cities
-- =====================================================================
INSERT INTO aml_city_risk_scores (tenant_id, city_id, region_id, name_ar, name_en, risk_level)
VALUES
    (seed_tenant, '06-001', '06', 'أبها', 'Abha', 'MEDIUM'),
    (seed_tenant, '06-002', '06', 'خميس مشيط', 'Khamis Mushait', 'MEDIUM'),
    (seed_tenant, '06-003', '06', 'بيشة', 'Bisha', 'MEDIUM'),
    (seed_tenant, '06-004', '06', 'النماص', 'An Namas', 'LOW'),
    (seed_tenant, '06-005', '06', 'سراة عبيدة', 'Sarat Abidah', 'MEDIUM'),
    (seed_tenant, '06-006', '06', 'رجال ألمع', 'Rijal Alma', 'LOW'),
    (seed_tenant, '06-007', '06', 'محايل عسير', 'Muhayil Asir', 'MEDIUM'),
    (seed_tenant, '06-008', '06', 'تثليث', 'Tathlith', 'LOW'),
    (seed_tenant, '06-009', '06', 'بلقرن', 'Balqarn', 'LOW'),
    (seed_tenant, '06-010', '06', 'ظهران الجنوب', 'Dhahran Al Janub', 'HIGH'),
    (seed_tenant, '06-011', '06', 'أحد رفيدة', 'Ahad Rafidah', 'LOW');

-- =====================================================================
-- Region 07: Tabuk Region (منطقة تبوك)
-- Near Jordan border — some border cities have elevated risk
-- =====================================================================
INSERT INTO aml_city_risk_scores (tenant_id, city_id, region_id, name_ar, name_en, risk_level)
VALUES
    (seed_tenant, '07-001', '07', 'تبوك', 'Tabuk', 'MEDIUM'),
    (seed_tenant, '07-002', '07', 'الوجه', 'Al Wajh', 'LOW'),
    (seed_tenant, '07-003', '07', 'ضباء', 'Duba', 'MEDIUM'),
    (seed_tenant, '07-004', '07', 'تيماء', 'Tayma', 'LOW'),
    (seed_tenant, '07-005', '07', 'حقل', 'Haql', 'HIGH'),
    (seed_tenant, '07-006', '07', 'أملج', 'Umluj Tabuk', 'LOW'),
    (seed_tenant, '07-007', '07', 'البدع', 'Al Bid', 'MEDIUM');

-- =====================================================================
-- Region 08: Ha'il Region (منطقة حائل)
-- =====================================================================
INSERT INTO aml_city_risk_scores (tenant_id, city_id, region_id, name_ar, name_en, risk_level)
VALUES
    (seed_tenant, '08-001', '08', 'حائل', 'Hail', 'LOW'),
    (seed_tenant, '08-002', '08', 'بقعاء', 'Baqaa', 'LOW'),
    (seed_tenant, '08-003', '08', 'الغزالة', 'Al Ghazalah', 'LOW'),
    (seed_tenant, '08-004', '08', 'الشملي', 'Ash Shamli', 'LOW'),
    (seed_tenant, '08-005', '08', 'الشنان', 'Ash Shinan', 'LOW'),
    (seed_tenant, '08-006', '08', 'موقق', 'Mawqaq', 'LOW');

-- =====================================================================
-- Region 09: Northern Borders Region (منطقة الحدود الشمالية)
-- Borders Iraq & Jordan — HIGH risk border zone
-- =====================================================================
INSERT INTO aml_city_risk_scores (tenant_id, city_id, region_id, name_ar, name_en, risk_level)
VALUES
    (seed_tenant, '09-001', '09', 'عرعر', 'Arar', 'HIGH'),
    (seed_tenant, '09-002', '09', 'طريف', 'Turayf', 'HIGH'),
    (seed_tenant, '09-003', '09', 'رفحاء', 'Rafha', 'HIGH'),
    (seed_tenant, '09-004', '09', 'العويقيلة', 'Al Uwayqilah', 'HIGH');

-- =====================================================================
-- Region 10: Jazan Region (منطقة جازان)
-- Borders Yemen (active conflict zone) — HIGH risk region
-- =====================================================================
INSERT INTO aml_city_risk_scores (tenant_id, city_id, region_id, name_ar, name_en, risk_level)
VALUES
    (seed_tenant, '10-001', '10', 'جازان', 'Jazan', 'HIGH'),
    (seed_tenant, '10-002', '10', 'صبيا', 'Sabya', 'HIGH'),
    (seed_tenant, '10-003', '10', 'أبو عريش', 'Abu Arish', 'HIGH'),
    (seed_tenant, '10-004', '10', 'صامطة', 'Samtah', 'HIGH'),
    (seed_tenant, '10-005', '10', 'الدرب', 'Al Darb', 'HIGH'),
    (seed_tenant, '10-006', '10', 'فرسان', 'Farasan', 'HIGH'),
    (seed_tenant, '10-007', '10', 'الحرث', 'Al Harth', 'HIGH'),
    (seed_tenant, '10-008', '10', 'العيدابي', 'Al Idabi', 'HIGH'),
    (seed_tenant, '10-009', '10', 'بيش', 'Baysh', 'HIGH'),
    (seed_tenant, '10-010', '10', 'الريث', 'Ar Rayth', 'HIGH'),
    (seed_tenant, '10-011', '10', 'فيفا', 'Fayfa', 'HIGH'),
    (seed_tenant, '10-012', '10', 'الدائر', 'Ad Dair', 'HIGH'),
    (seed_tenant, '10-013', '10', 'الطوال', 'At Tuwal', 'HIGH'),
    (seed_tenant, '10-014', '10', 'هروب', 'Haroub', 'HIGH');

-- =====================================================================
-- Region 11: Najran Region (منطقة نجران)
-- Borders Yemen — HIGH risk border zone
-- =====================================================================
INSERT INTO aml_city_risk_scores (tenant_id, city_id, region_id, name_ar, name_en, risk_level)
VALUES
    (seed_tenant, '11-001', '11', 'نجران', 'Najran', 'HIGH'),
    (seed_tenant, '11-002', '11', 'شرورة', 'Sharurah', 'HIGH'),
    (seed_tenant, '11-003', '11', 'حبونا', 'Hubuna', 'HIGH'),
    (seed_tenant, '11-004', '11', 'يدمة', 'Yadamah', 'HIGH'),
    (seed_tenant, '11-005', '11', 'بدر الجنوب', 'Badr Al Janub', 'HIGH'),
    (seed_tenant, '11-006', '11', 'الخرخير', 'Al Kharkhir', 'HIGH');

-- =====================================================================
-- Region 12: Al Bahah Region (منطقة الباحة)
-- Interior mountain region — LOW risk
-- =====================================================================
INSERT INTO aml_city_risk_scores (tenant_id, city_id, region_id, name_ar, name_en, risk_level)
VALUES
    (seed_tenant, '12-001', '12', 'الباحة', 'Al Bahah', 'LOW'),
    (seed_tenant, '12-002', '12', 'بلجرشي', 'Baljurashi', 'LOW'),
    (seed_tenant, '12-003', '12', 'المندق', 'Al Mandaq', 'LOW'),
    (seed_tenant, '12-004', '12', 'المخواة', 'Al Makhwah', 'LOW'),
    (seed_tenant, '12-005', '12', 'العقيق', 'Al Aqiq', 'LOW'),
    (seed_tenant, '12-006', '12', 'قلوة', 'Qilwah', 'LOW');

-- =====================================================================
-- Region 13: Al Jawf Region (منطقة الجوف)
-- Borders Jordan & Iraq — elevated risk
-- =====================================================================
INSERT INTO aml_city_risk_scores (tenant_id, city_id, region_id, name_ar, name_en, risk_level)
VALUES
    (seed_tenant, '13-001', '13', 'سكاكا', 'Sakaka', 'MEDIUM'),
    (seed_tenant, '13-002', '13', 'القريات', 'Al Qurayyat', 'HIGH'),
    (seed_tenant, '13-003', '13', 'دومة الجندل', 'Dawmat Al Jandal', 'MEDIUM'),
    (seed_tenant, '13-004', '13', 'طبرجل', 'Tabarjal', 'MEDIUM');

-- =====================================================================
-- Summary:
--   Total cities: 120
--   HIGH risk:  29 (border cities near Yemen, Iraq, Jordan)
--   MEDIUM risk: 27 (major commercial/financial/pilgrimage centers)
--   LOW risk:   64 (interior residential and agricultural cities)
-- =====================================================================

END $$;
