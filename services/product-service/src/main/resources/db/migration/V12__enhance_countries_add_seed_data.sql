-- V12__enhance_countries_add_seed_data.sql
-- Enhance countries table with additional fields for Islamic financing platform
-- and seed comprehensive country data for KSA project scenarios.

-- =============================================
-- 1. ADD NEW COLUMNS
-- =============================================
ALTER TABLE countries ADD COLUMN IF NOT EXISTS alpha3_code     VARCHAR(3);
ALTER TABLE countries ADD COLUMN IF NOT EXISTS numeric_code    VARCHAR(3);
ALTER TABLE countries ADD COLUMN IF NOT EXISTS slug            VARCHAR(100);
ALTER TABLE countries ADD COLUMN IF NOT EXISTS nationality_en  VARCHAR(100);
ALTER TABLE countries ADD COLUMN IF NOT EXISTS nationality_ar  VARCHAR(100);
ALTER TABLE countries ADD COLUMN IF NOT EXISTS currency_name_en VARCHAR(100);
ALTER TABLE countries ADD COLUMN IF NOT EXISTS currency_name_ar VARCHAR(100);
ALTER TABLE countries ADD COLUMN IF NOT EXISTS flag_emoji      VARCHAR(10);
ALTER TABLE countries ADD COLUMN IF NOT EXISTS capital_en      VARCHAR(100);
ALTER TABLE countries ADD COLUMN IF NOT EXISTS capital_ar      VARCHAR(100);
ALTER TABLE countries ADD COLUMN IF NOT EXISTS region          VARCHAR(50);
ALTER TABLE countries ADD COLUMN IF NOT EXISTS sub_region      VARCHAR(50);
ALTER TABLE countries ADD COLUMN IF NOT EXISTS is_sanctioned   BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE countries ADD COLUMN IF NOT EXISTS risk_tier       VARCHAR(20) NOT NULL DEFAULT 'STANDARD';
ALTER TABLE countries ADD COLUMN IF NOT EXISTS is_arab_league  BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE countries ADD COLUMN IF NOT EXISTS is_oic_member   BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE countries ADD COLUMN IF NOT EXISTS iban_required   BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE countries ADD COLUMN IF NOT EXISTS iban_length     INT;

-- Add unique constraint on alpha3_code per tenant
ALTER TABLE countries ADD CONSTRAINT uq_country_tenant_alpha3 UNIQUE (tenant_id, alpha3_code);

-- Add index for sanctioned country lookup
CREATE INDEX idx_countries_sanctioned ON countries(tenant_id, is_sanctioned) WHERE is_sanctioned = TRUE;

-- Add index for risk tier filtering
CREATE INDEX idx_countries_risk_tier ON countries(tenant_id, risk_tier);

-- Add index for region filtering
CREATE INDEX idx_countries_region ON countries(tenant_id, region) WHERE is_active = TRUE;

-- Add index for slug lookup
CREATE INDEX idx_countries_slug ON countries(tenant_id, slug);

-- =============================================
-- 2. DELETE EXISTING SEED DATA (if any)
-- =============================================
DELETE FROM countries WHERE tenant_id = '00000000-0000-0000-0000-000000000001';

-- =============================================
-- 3. SEED COMPREHENSIVE COUNTRY DATA
-- =============================================
-- Risk Tiers: LOW, STANDARD, ELEVATED, HIGH, PROHIBITED
-- Regions: GCC, Middle East, North Africa, Sub-Saharan Africa, South Asia, Southeast Asia, East Asia, Central Asia, Europe, North America, South America, Oceania

INSERT INTO countries (tenant_id, code, alpha3_code, numeric_code, slug, name_en, name_ar, nationality_en, nationality_ar, dial_code, currency_code, currency_name_en, currency_name_ar, flag_emoji, capital_en, capital_ar, region, sub_region, is_gcc, is_arab_league, is_oic_member, is_sanctioned, risk_tier, iban_required, iban_length, is_active, sort_order) VALUES

-- ============ GCC COUNTRIES (6) ============
('00000000-0000-0000-0000-000000000001', 'SA', 'SAU', '682', 'saudi-arabia', 'Saudi Arabia', 'المملكة العربية السعودية', 'Saudi', 'سعودي', '+966', 'SAR', 'Saudi Riyal', 'ريال سعودي', '🇸🇦', 'Riyadh', 'الرياض', 'GCC', 'Arabian Peninsula', TRUE, TRUE, TRUE, FALSE, 'LOW', TRUE, 24, TRUE, 1),
('00000000-0000-0000-0000-000000000001', 'AE', 'ARE', '784', 'united-arab-emirates', 'United Arab Emirates', 'الإمارات العربية المتحدة', 'Emirati', 'إماراتي', '+971', 'AED', 'UAE Dirham', 'درهم إماراتي', '🇦🇪', 'Abu Dhabi', 'أبو ظبي', 'GCC', 'Arabian Peninsula', TRUE, TRUE, TRUE, FALSE, 'LOW', TRUE, 23, TRUE, 2),
('00000000-0000-0000-0000-000000000001', 'BH', 'BHR', '048', 'bahrain', 'Bahrain', 'البحرين', 'Bahraini', 'بحريني', '+973', 'BHD', 'Bahraini Dinar', 'دينار بحريني', '🇧🇭', 'Manama', 'المنامة', 'GCC', 'Arabian Peninsula', TRUE, TRUE, TRUE, FALSE, 'LOW', TRUE, 22, TRUE, 3),
('00000000-0000-0000-0000-000000000001', 'KW', 'KWT', '414', 'kuwait', 'Kuwait', 'الكويت', 'Kuwaiti', 'كويتي', '+965', 'KWD', 'Kuwaiti Dinar', 'دينار كويتي', '🇰🇼', 'Kuwait City', 'مدينة الكويت', 'GCC', 'Arabian Peninsula', TRUE, TRUE, TRUE, FALSE, 'LOW', TRUE, 30, TRUE, 4),
('00000000-0000-0000-0000-000000000001', 'OM', 'OMN', '512', 'oman', 'Oman', 'عُمان', 'Omani', 'عُماني', '+968', 'OMR', 'Omani Rial', 'ريال عُماني', '🇴🇲', 'Muscat', 'مسقط', 'GCC', 'Arabian Peninsula', TRUE, TRUE, TRUE, FALSE, 'LOW', TRUE, 23, TRUE, 5),
('00000000-0000-0000-0000-000000000001', 'QA', 'QAT', '634', 'qatar', 'Qatar', 'قطر', 'Qatari', 'قطري', '+974', 'QAR', 'Qatari Riyal', 'ريال قطري', '🇶🇦', 'Doha', 'الدوحة', 'GCC', 'Arabian Peninsula', TRUE, TRUE, TRUE, FALSE, 'LOW', TRUE, 29, TRUE, 6),

-- ============ ARAB LEAGUE (non-GCC) (16) ============
('00000000-0000-0000-0000-000000000001', 'EG', 'EGY', '818', 'egypt', 'Egypt', 'مصر', 'Egyptian', 'مصري', '+20', 'EGP', 'Egyptian Pound', 'جنيه مصري', '🇪🇬', 'Cairo', 'القاهرة', 'Middle East', 'North Africa', FALSE, TRUE, TRUE, FALSE, 'STANDARD', TRUE, 29, TRUE, 10),
('00000000-0000-0000-0000-000000000001', 'JO', 'JOR', '400', 'jordan', 'Jordan', 'الأردن', 'Jordanian', 'أردني', '+962', 'JOD', 'Jordanian Dinar', 'دينار أردني', '🇯🇴', 'Amman', 'عمّان', 'Middle East', 'Western Asia', FALSE, TRUE, TRUE, FALSE, 'LOW', TRUE, 30, TRUE, 11),
('00000000-0000-0000-0000-000000000001', 'LB', 'LBN', '422', 'lebanon', 'Lebanon', 'لبنان', 'Lebanese', 'لبناني', '+961', 'LBP', 'Lebanese Pound', 'ليرة لبنانية', '🇱🇧', 'Beirut', 'بيروت', 'Middle East', 'Western Asia', FALSE, TRUE, TRUE, FALSE, 'ELEVATED', TRUE, 28, TRUE, 12),
('00000000-0000-0000-0000-000000000001', 'IQ', 'IRQ', '368', 'iraq', 'Iraq', 'العراق', 'Iraqi', 'عراقي', '+964', 'IQD', 'Iraqi Dinar', 'دينار عراقي', '🇮🇶', 'Baghdad', 'بغداد', 'Middle East', 'Western Asia', FALSE, TRUE, TRUE, FALSE, 'HIGH', TRUE, 23, TRUE, 13),
('00000000-0000-0000-0000-000000000001', 'PS', 'PSE', '275', 'palestine', 'Palestine', 'فلسطين', 'Palestinian', 'فلسطيني', '+970', 'ILS', 'Israeli Shekel', 'شيكل إسرائيلي', '🇵🇸', 'Ramallah', 'رام الله', 'Middle East', 'Western Asia', FALSE, TRUE, TRUE, FALSE, 'ELEVATED', FALSE, NULL, TRUE, 14),
('00000000-0000-0000-0000-000000000001', 'SD', 'SDN', '729', 'sudan', 'Sudan', 'السودان', 'Sudanese', 'سوداني', '+249', 'SDG', 'Sudanese Pound', 'جنيه سوداني', '🇸🇩', 'Khartoum', 'الخرطوم', 'Middle East', 'North Africa', FALSE, TRUE, TRUE, FALSE, 'HIGH', FALSE, NULL, TRUE, 15),
('00000000-0000-0000-0000-000000000001', 'LY', 'LBY', '434', 'libya', 'Libya', 'ليبيا', 'Libyan', 'ليبي', '+218', 'LYD', 'Libyan Dinar', 'دينار ليبي', '🇱🇾', 'Tripoli', 'طرابلس', 'Middle East', 'North Africa', FALSE, TRUE, TRUE, FALSE, 'HIGH', FALSE, NULL, TRUE, 16),
('00000000-0000-0000-0000-000000000001', 'TN', 'TUN', '788', 'tunisia', 'Tunisia', 'تونس', 'Tunisian', 'تونسي', '+216', 'TND', 'Tunisian Dinar', 'دينار تونسي', '🇹🇳', 'Tunis', 'تونس العاصمة', 'Middle East', 'North Africa', FALSE, TRUE, TRUE, FALSE, 'STANDARD', TRUE, 24, TRUE, 17),
('00000000-0000-0000-0000-000000000001', 'DZ', 'DZA', '012', 'algeria', 'Algeria', 'الجزائر', 'Algerian', 'جزائري', '+213', 'DZD', 'Algerian Dinar', 'دينار جزائري', '🇩🇿', 'Algiers', 'الجزائر العاصمة', 'Middle East', 'North Africa', FALSE, TRUE, TRUE, FALSE, 'STANDARD', FALSE, NULL, TRUE, 18),
('00000000-0000-0000-0000-000000000001', 'MA', 'MAR', '504', 'morocco', 'Morocco', 'المغرب', 'Moroccan', 'مغربي', '+212', 'MAD', 'Moroccan Dirham', 'درهم مغربي', '🇲🇦', 'Rabat', 'الرباط', 'Middle East', 'North Africa', FALSE, TRUE, TRUE, FALSE, 'STANDARD', TRUE, 28, TRUE, 19),
('00000000-0000-0000-0000-000000000001', 'MR', 'MRT', '478', 'mauritania', 'Mauritania', 'موريتانيا', 'Mauritanian', 'موريتاني', '+222', 'MRU', 'Mauritanian Ouguiya', 'أوقية موريتانية', '🇲🇷', 'Nouakchott', 'نواكشوط', 'Middle East', 'West Africa', FALSE, TRUE, TRUE, FALSE, 'ELEVATED', FALSE, NULL, TRUE, 20),
('00000000-0000-0000-0000-000000000001', 'SO', 'SOM', '706', 'somalia', 'Somalia', 'الصومال', 'Somali', 'صومالي', '+252', 'SOS', 'Somali Shilling', 'شلن صومالي', '🇸🇴', 'Mogadishu', 'مقديشو', 'Middle East', 'East Africa', FALSE, TRUE, TRUE, FALSE, 'HIGH', FALSE, NULL, TRUE, 21),
('00000000-0000-0000-0000-000000000001', 'DJ', 'DJI', '262', 'djibouti', 'Djibouti', 'جيبوتي', 'Djiboutian', 'جيبوتي', '+253', 'DJF', 'Djiboutian Franc', 'فرنك جيبوتي', '🇩🇯', 'Djibouti', 'جيبوتي', 'Middle East', 'East Africa', FALSE, TRUE, TRUE, FALSE, 'ELEVATED', FALSE, NULL, TRUE, 22),
('00000000-0000-0000-0000-000000000001', 'KM', 'COM', '174', 'comoros', 'Comoros', 'جزر القمر', 'Comorian', 'قمري', '+269', 'KMF', 'Comorian Franc', 'فرنك قمري', '🇰🇲', 'Moroni', 'موروني', 'Middle East', 'East Africa', FALSE, TRUE, TRUE, FALSE, 'ELEVATED', FALSE, NULL, TRUE, 23),
('00000000-0000-0000-0000-000000000001', 'SY', 'SYR', '760', 'syria', 'Syria', 'سوريا', 'Syrian', 'سوري', '+963', 'SYP', 'Syrian Pound', 'ليرة سورية', '🇸🇾', 'Damascus', 'دمشق', 'Middle East', 'Western Asia', FALSE, TRUE, TRUE, TRUE, 'PROHIBITED', FALSE, NULL, FALSE, 24),
('00000000-0000-0000-0000-000000000001', 'YE', 'YEM', '887', 'yemen', 'Yemen', 'اليمن', 'Yemeni', 'يمني', '+967', 'YER', 'Yemeni Rial', 'ريال يمني', '🇾🇪', 'Sanaa', 'صنعاء', 'Middle East', 'Arabian Peninsula', FALSE, TRUE, TRUE, FALSE, 'HIGH', FALSE, NULL, TRUE, 25),

-- ============ KEY EXPAT NATIONALITIES IN KSA (10) ============
('00000000-0000-0000-0000-000000000001', 'PK', 'PAK', '586', 'pakistan', 'Pakistan', 'باكستان', 'Pakistani', 'باكستاني', '+92', 'PKR', 'Pakistani Rupee', 'روبية باكستانية', '🇵🇰', 'Islamabad', 'إسلام آباد', 'South Asia', 'Southern Asia', FALSE, FALSE, TRUE, FALSE, 'STANDARD', TRUE, 24, TRUE, 30),
('00000000-0000-0000-0000-000000000001', 'IN', 'IND', '356', 'india', 'India', 'الهند', 'Indian', 'هندي', '+91', 'INR', 'Indian Rupee', 'روبية هندية', '🇮🇳', 'New Delhi', 'نيودلهي', 'South Asia', 'Southern Asia', FALSE, FALSE, FALSE, FALSE, 'STANDARD', FALSE, NULL, TRUE, 31),
('00000000-0000-0000-0000-000000000001', 'BD', 'BGD', '050', 'bangladesh', 'Bangladesh', 'بنغلاديش', 'Bangladeshi', 'بنغلاديشي', '+880', 'BDT', 'Bangladeshi Taka', 'تاكا بنغلاديشية', '🇧🇩', 'Dhaka', 'دكا', 'South Asia', 'Southern Asia', FALSE, FALSE, TRUE, FALSE, 'STANDARD', FALSE, NULL, TRUE, 32),
('00000000-0000-0000-0000-000000000001', 'PH', 'PHL', '608', 'philippines', 'Philippines', 'الفلبين', 'Filipino', 'فلبيني', '+63', 'PHP', 'Philippine Peso', 'بيزو فلبيني', '🇵🇭', 'Manila', 'مانيلا', 'Southeast Asia', 'South-Eastern Asia', FALSE, FALSE, TRUE, FALSE, 'STANDARD', FALSE, NULL, TRUE, 33),
('00000000-0000-0000-0000-000000000001', 'ID', 'IDN', '360', 'indonesia', 'Indonesia', 'إندونيسيا', 'Indonesian', 'إندونيسي', '+62', 'IDR', 'Indonesian Rupiah', 'روبية إندونيسية', '🇮🇩', 'Jakarta', 'جاكرتا', 'Southeast Asia', 'South-Eastern Asia', FALSE, FALSE, TRUE, FALSE, 'STANDARD', FALSE, NULL, TRUE, 34),
('00000000-0000-0000-0000-000000000001', 'NP', 'NPL', '524', 'nepal', 'Nepal', 'نيبال', 'Nepalese', 'نيبالي', '+977', 'NPR', 'Nepalese Rupee', 'روبية نيبالية', '🇳🇵', 'Kathmandu', 'كاتماندو', 'South Asia', 'Southern Asia', FALSE, FALSE, TRUE, FALSE, 'STANDARD', FALSE, NULL, TRUE, 35),
('00000000-0000-0000-0000-000000000001', 'LK', 'LKA', '144', 'sri-lanka', 'Sri Lanka', 'سريلانكا', 'Sri Lankan', 'سريلانكي', '+94', 'LKR', 'Sri Lankan Rupee', 'روبية سريلانكية', '🇱🇰', 'Colombo', 'كولومبو', 'South Asia', 'Southern Asia', FALSE, FALSE, TRUE, FALSE, 'STANDARD', FALSE, NULL, TRUE, 36),
('00000000-0000-0000-0000-000000000001', 'ET', 'ETH', '231', 'ethiopia', 'Ethiopia', 'إثيوبيا', 'Ethiopian', 'إثيوبي', '+251', 'ETB', 'Ethiopian Birr', 'بر إثيوبي', '🇪🇹', 'Addis Ababa', 'أديس أبابا', 'Sub-Saharan Africa', 'East Africa', FALSE, FALSE, FALSE, FALSE, 'STANDARD', FALSE, NULL, TRUE, 37),
('00000000-0000-0000-0000-000000000001', 'ER', 'ERI', '232', 'eritrea', 'Eritrea', 'إريتريا', 'Eritrean', 'إريتري', '+291', 'ERN', 'Eritrean Nakfa', 'ناكفا إريترية', '🇪🇷', 'Asmara', 'أسمرة', 'Sub-Saharan Africa', 'East Africa', FALSE, FALSE, FALSE, FALSE, 'ELEVATED', FALSE, NULL, TRUE, 38),
('00000000-0000-0000-0000-000000000001', 'MM', 'MMR', '104', 'myanmar', 'Myanmar', 'ميانمار', 'Myanmar', 'ميانماري', '+95', 'MMK', 'Myanmar Kyat', 'كيات ميانماري', '🇲🇲', 'Naypyidaw', 'نايبيداو', 'Southeast Asia', 'South-Eastern Asia', FALSE, FALSE, TRUE, FALSE, 'HIGH', FALSE, NULL, TRUE, 39),

-- ============ ISLAMIC FINANCE HUBS (5) ============
('00000000-0000-0000-0000-000000000001', 'MY', 'MYS', '458', 'malaysia', 'Malaysia', 'ماليزيا', 'Malaysian', 'ماليزي', '+60', 'MYR', 'Malaysian Ringgit', 'رينغيت ماليزي', '🇲🇾', 'Kuala Lumpur', 'كوالالمبور', 'Southeast Asia', 'South-Eastern Asia', FALSE, FALSE, TRUE, FALSE, 'LOW', FALSE, NULL, TRUE, 40),
('00000000-0000-0000-0000-000000000001', 'TR', 'TUR', '792', 'turkey', 'Turkey', 'تركيا', 'Turkish', 'تركي', '+90', 'TRY', 'Turkish Lira', 'ليرة تركية', '🇹🇷', 'Ankara', 'أنقرة', 'Europe', 'Western Asia', FALSE, FALSE, TRUE, FALSE, 'STANDARD', TRUE, 26, TRUE, 41),
('00000000-0000-0000-0000-000000000001', 'BN', 'BRN', '096', 'brunei', 'Brunei', 'بروناي', 'Bruneian', 'بروناوي', '+673', 'BND', 'Brunei Dollar', 'دولار بروناي', '🇧🇳', 'Bandar Seri Begawan', 'بندر سري بيغاوان', 'Southeast Asia', 'South-Eastern Asia', FALSE, FALSE, TRUE, FALSE, 'LOW', FALSE, NULL, TRUE, 42),
('00000000-0000-0000-0000-000000000001', 'MV', 'MDV', '462', 'maldives', 'Maldives', 'المالديف', 'Maldivian', 'مالديفي', '+960', 'MVR', 'Maldivian Rufiyaa', 'روفية مالديفية', '🇲🇻', 'Male', 'ماليه', 'South Asia', 'Southern Asia', FALSE, FALSE, TRUE, FALSE, 'STANDARD', FALSE, NULL, TRUE, 43),
('00000000-0000-0000-0000-000000000001', 'NG', 'NGA', '566', 'nigeria', 'Nigeria', 'نيجيريا', 'Nigerian', 'نيجيري', '+234', 'NGN', 'Nigerian Naira', 'نايرا نيجيرية', '🇳🇬', 'Abuja', 'أبوجا', 'Sub-Saharan Africa', 'West Africa', FALSE, FALSE, TRUE, FALSE, 'ELEVATED', FALSE, NULL, TRUE, 44),

-- ============ MAJOR ECONOMIES / G20 (12) ============
('00000000-0000-0000-0000-000000000001', 'US', 'USA', '840', 'united-states', 'United States', 'الولايات المتحدة', 'American', 'أمريكي', '+1', 'USD', 'US Dollar', 'دولار أمريكي', '🇺🇸', 'Washington D.C.', 'واشنطن', 'North America', 'Northern America', FALSE, FALSE, FALSE, FALSE, 'LOW', FALSE, NULL, TRUE, 50),
('00000000-0000-0000-0000-000000000001', 'GB', 'GBR', '826', 'united-kingdom', 'United Kingdom', 'المملكة المتحدة', 'British', 'بريطاني', '+44', 'GBP', 'British Pound', 'جنيه إسترليني', '🇬🇧', 'London', 'لندن', 'Europe', 'Northern Europe', FALSE, FALSE, FALSE, FALSE, 'LOW', TRUE, 22, TRUE, 51),
('00000000-0000-0000-0000-000000000001', 'DE', 'DEU', '276', 'germany', 'Germany', 'ألمانيا', 'German', 'ألماني', '+49', 'EUR', 'Euro', 'يورو', '🇩🇪', 'Berlin', 'برلين', 'Europe', 'Western Europe', FALSE, FALSE, FALSE, FALSE, 'LOW', TRUE, 22, TRUE, 52),
('00000000-0000-0000-0000-000000000001', 'FR', 'FRA', '250', 'france', 'France', 'فرنسا', 'French', 'فرنسي', '+33', 'EUR', 'Euro', 'يورو', '🇫🇷', 'Paris', 'باريس', 'Europe', 'Western Europe', FALSE, FALSE, FALSE, FALSE, 'LOW', TRUE, 27, TRUE, 53),
('00000000-0000-0000-0000-000000000001', 'JP', 'JPN', '392', 'japan', 'Japan', 'اليابان', 'Japanese', 'ياباني', '+81', 'JPY', 'Japanese Yen', 'ين ياباني', '🇯🇵', 'Tokyo', 'طوكيو', 'East Asia', 'Eastern Asia', FALSE, FALSE, FALSE, FALSE, 'LOW', FALSE, NULL, TRUE, 54),
('00000000-0000-0000-0000-000000000001', 'CN', 'CHN', '156', 'china', 'China', 'الصين', 'Chinese', 'صيني', '+86', 'CNY', 'Chinese Yuan', 'يوان صيني', '🇨🇳', 'Beijing', 'بكين', 'East Asia', 'Eastern Asia', FALSE, FALSE, FALSE, FALSE, 'STANDARD', FALSE, NULL, TRUE, 55),
('00000000-0000-0000-0000-000000000001', 'KR', 'KOR', '410', 'south-korea', 'South Korea', 'كوريا الجنوبية', 'South Korean', 'كوري جنوبي', '+82', 'KRW', 'South Korean Won', 'وون كوري جنوبي', '🇰🇷', 'Seoul', 'سيول', 'East Asia', 'Eastern Asia', FALSE, FALSE, FALSE, FALSE, 'LOW', FALSE, NULL, TRUE, 56),
('00000000-0000-0000-0000-000000000001', 'CA', 'CAN', '124', 'canada', 'Canada', 'كندا', 'Canadian', 'كندي', '+1', 'CAD', 'Canadian Dollar', 'دولار كندي', '🇨🇦', 'Ottawa', 'أوتاوا', 'North America', 'Northern America', FALSE, FALSE, FALSE, FALSE, 'LOW', FALSE, NULL, TRUE, 57),
('00000000-0000-0000-0000-000000000001', 'AU', 'AUS', '036', 'australia', 'Australia', 'أستراليا', 'Australian', 'أسترالي', '+61', 'AUD', 'Australian Dollar', 'دولار أسترالي', '🇦🇺', 'Canberra', 'كانبيرا', 'Oceania', 'Australasia', FALSE, FALSE, FALSE, FALSE, 'LOW', FALSE, NULL, TRUE, 58),
('00000000-0000-0000-0000-000000000001', 'IT', 'ITA', '380', 'italy', 'Italy', 'إيطاليا', 'Italian', 'إيطالي', '+39', 'EUR', 'Euro', 'يورو', '🇮🇹', 'Rome', 'روما', 'Europe', 'Southern Europe', FALSE, FALSE, FALSE, FALSE, 'LOW', TRUE, 27, TRUE, 59),
('00000000-0000-0000-0000-000000000001', 'BR', 'BRA', '076', 'brazil', 'Brazil', 'البرازيل', 'Brazilian', 'برازيلي', '+55', 'BRL', 'Brazilian Real', 'ريال برازيلي', '🇧🇷', 'Brasilia', 'برازيليا', 'South America', 'South America', FALSE, FALSE, FALSE, FALSE, 'STANDARD', FALSE, NULL, TRUE, 60),
('00000000-0000-0000-0000-000000000001', 'MX', 'MEX', '484', 'mexico', 'Mexico', 'المكسيك', 'Mexican', 'مكسيكي', '+52', 'MXN', 'Mexican Peso', 'بيزو مكسيكي', '🇲🇽', 'Mexico City', 'مكسيكو سيتي', 'North America', 'Central America', FALSE, FALSE, FALSE, FALSE, 'STANDARD', TRUE, 18, TRUE, 61),

-- ============ EUROPEAN FINANCIAL CENTERS (5) ============
('00000000-0000-0000-0000-000000000001', 'CH', 'CHE', '756', 'switzerland', 'Switzerland', 'سويسرا', 'Swiss', 'سويسري', '+41', 'CHF', 'Swiss Franc', 'فرنك سويسري', '🇨🇭', 'Bern', 'برن', 'Europe', 'Western Europe', FALSE, FALSE, FALSE, FALSE, 'LOW', TRUE, 21, TRUE, 62),
('00000000-0000-0000-0000-000000000001', 'NL', 'NLD', '528', 'netherlands', 'Netherlands', 'هولندا', 'Dutch', 'هولندي', '+31', 'EUR', 'Euro', 'يورو', '🇳🇱', 'Amsterdam', 'أمستردام', 'Europe', 'Western Europe', FALSE, FALSE, FALSE, FALSE, 'LOW', TRUE, 18, TRUE, 63),
('00000000-0000-0000-0000-000000000001', 'SG', 'SGP', '702', 'singapore', 'Singapore', 'سنغافورة', 'Singaporean', 'سنغافوري', '+65', 'SGD', 'Singapore Dollar', 'دولار سنغافوري', '🇸🇬', 'Singapore', 'سنغافورة', 'Southeast Asia', 'South-Eastern Asia', FALSE, FALSE, FALSE, FALSE, 'LOW', FALSE, NULL, TRUE, 64),
('00000000-0000-0000-0000-000000000001', 'HK', 'HKG', '344', 'hong-kong', 'Hong Kong', 'هونغ كونغ', 'Hong Konger', 'هونغ كونغي', '+852', 'HKD', 'Hong Kong Dollar', 'دولار هونغ كونغ', '🇭🇰', 'Hong Kong', 'هونغ كونغ', 'East Asia', 'Eastern Asia', FALSE, FALSE, FALSE, FALSE, 'LOW', FALSE, NULL, TRUE, 65),
('00000000-0000-0000-0000-000000000001', 'LU', 'LUX', '442', 'luxembourg', 'Luxembourg', 'لوكسمبورغ', 'Luxembourgish', 'لوكسمبورغي', '+352', 'EUR', 'Euro', 'يورو', '🇱🇺', 'Luxembourg', 'لوكسمبورغ', 'Europe', 'Western Europe', FALSE, FALSE, FALSE, FALSE, 'LOW', TRUE, 20, TRUE, 66),

-- ============ ADDITIONAL OIC / MUSLIM-MAJORITY (8) ============
('00000000-0000-0000-0000-000000000001', 'AF', 'AFG', '004', 'afghanistan', 'Afghanistan', 'أفغانستان', 'Afghan', 'أفغاني', '+93', 'AFN', 'Afghan Afghani', 'أفغاني أفغانستاني', '🇦🇫', 'Kabul', 'كابل', 'Central Asia', 'Southern Asia', FALSE, FALSE, TRUE, TRUE, 'PROHIBITED', FALSE, NULL, FALSE, 70),
('00000000-0000-0000-0000-000000000001', 'AZ', 'AZE', '031', 'azerbaijan', 'Azerbaijan', 'أذربيجان', 'Azerbaijani', 'أذربيجاني', '+994', 'AZN', 'Azerbaijani Manat', 'مانات أذربيجاني', '🇦🇿', 'Baku', 'باكو', 'Central Asia', 'Western Asia', FALSE, FALSE, TRUE, FALSE, 'STANDARD', TRUE, 28, TRUE, 71),
('00000000-0000-0000-0000-000000000001', 'UZ', 'UZB', '860', 'uzbekistan', 'Uzbekistan', 'أوزبكستان', 'Uzbek', 'أوزبكي', '+998', 'UZS', 'Uzbekistani Som', 'سوم أوزبكستاني', '🇺🇿', 'Tashkent', 'طشقند', 'Central Asia', 'Central Asia', FALSE, FALSE, TRUE, FALSE, 'STANDARD', FALSE, NULL, TRUE, 72),
('00000000-0000-0000-0000-000000000001', 'KZ', 'KAZ', '398', 'kazakhstan', 'Kazakhstan', 'كازاخستان', 'Kazakh', 'كازاخستاني', '+7', 'KZT', 'Kazakhstani Tenge', 'تنغي كازاخستاني', '🇰🇿', 'Astana', 'أستانا', 'Central Asia', 'Central Asia', FALSE, FALSE, TRUE, FALSE, 'STANDARD', TRUE, 20, TRUE, 73),
('00000000-0000-0000-0000-000000000001', 'SN', 'SEN', '686', 'senegal', 'Senegal', 'السنغال', 'Senegalese', 'سنغالي', '+221', 'XOF', 'West African CFA Franc', 'فرنك غرب أفريقي', '🇸🇳', 'Dakar', 'داكار', 'Sub-Saharan Africa', 'West Africa', FALSE, FALSE, TRUE, FALSE, 'STANDARD', FALSE, NULL, TRUE, 74),
('00000000-0000-0000-0000-000000000001', 'TJ', 'TJK', '762', 'tajikistan', 'Tajikistan', 'طاجيكستان', 'Tajik', 'طاجيكي', '+992', 'TJS', 'Tajikistani Somoni', 'سوموني طاجيكستاني', '🇹🇯', 'Dushanbe', 'دوشنبه', 'Central Asia', 'Central Asia', FALSE, FALSE, TRUE, FALSE, 'STANDARD', FALSE, NULL, TRUE, 75),
('00000000-0000-0000-0000-000000000001', 'TM', 'TKM', '795', 'turkmenistan', 'Turkmenistan', 'تركمانستان', 'Turkmen', 'تركماني', '+993', 'TMT', 'Turkmenistani Manat', 'مانات تركمانستاني', '🇹🇲', 'Ashgabat', 'عشق آباد', 'Central Asia', 'Central Asia', FALSE, FALSE, TRUE, FALSE, 'ELEVATED', FALSE, NULL, TRUE, 76),
('00000000-0000-0000-0000-000000000001', 'KG', 'KGZ', '417', 'kyrgyzstan', 'Kyrgyzstan', 'قيرغيزستان', 'Kyrgyz', 'قيرغيزي', '+996', 'KGS', 'Kyrgyzstani Som', 'سوم قيرغيزستاني', '🇰🇬', 'Bishkek', 'بيشكك', 'Central Asia', 'Central Asia', FALSE, FALSE, TRUE, FALSE, 'STANDARD', FALSE, NULL, TRUE, 77),

-- ============ SANCTIONED / HIGH-RISK COUNTRIES (6) ============
('00000000-0000-0000-0000-000000000001', 'IR', 'IRN', '364', 'iran', 'Iran', 'إيران', 'Iranian', 'إيراني', '+98', 'IRR', 'Iranian Rial', 'ريال إيراني', '🇮🇷', 'Tehran', 'طهران', 'Middle East', 'Southern Asia', FALSE, FALSE, TRUE, TRUE, 'PROHIBITED', FALSE, NULL, FALSE, 90),
('00000000-0000-0000-0000-000000000001', 'KP', 'PRK', '408', 'north-korea', 'North Korea', 'كوريا الشمالية', 'North Korean', 'كوري شمالي', '+850', 'KPW', 'North Korean Won', 'وون كوري شمالي', '🇰🇵', 'Pyongyang', 'بيونغ يانغ', 'East Asia', 'Eastern Asia', FALSE, FALSE, FALSE, TRUE, 'PROHIBITED', FALSE, NULL, FALSE, 91),
('00000000-0000-0000-0000-000000000001', 'CU', 'CUB', '192', 'cuba', 'Cuba', 'كوبا', 'Cuban', 'كوبي', '+53', 'CUP', 'Cuban Peso', 'بيزو كوبي', '🇨🇺', 'Havana', 'هافانا', 'North America', 'Caribbean', FALSE, FALSE, FALSE, TRUE, 'PROHIBITED', FALSE, NULL, FALSE, 92),
('00000000-0000-0000-0000-000000000001', 'RU', 'RUS', '643', 'russia', 'Russia', 'روسيا', 'Russian', 'روسي', '+7', 'RUB', 'Russian Ruble', 'روبل روسي', '🇷🇺', 'Moscow', 'موسكو', 'Europe', 'Eastern Europe', FALSE, FALSE, FALSE, TRUE, 'HIGH', FALSE, NULL, TRUE, 93),
('00000000-0000-0000-0000-000000000001', 'BY', 'BLR', '112', 'belarus', 'Belarus', 'بيلاروسيا', 'Belarusian', 'بيلاروسي', '+375', 'BYN', 'Belarusian Ruble', 'روبل بيلاروسي', '🇧🇾', 'Minsk', 'مينسك', 'Europe', 'Eastern Europe', FALSE, FALSE, FALSE, TRUE, 'HIGH', FALSE, NULL, TRUE, 94),
('00000000-0000-0000-0000-000000000001', 'VE', 'VEN', '862', 'venezuela', 'Venezuela', 'فنزويلا', 'Venezuelan', 'فنزويلي', '+58', 'VES', 'Venezuelan Bolivar', 'بوليفار فنزويلي', '🇻🇪', 'Caracas', 'كاراكاس', 'South America', 'South America', FALSE, FALSE, FALSE, TRUE, 'HIGH', FALSE, NULL, TRUE, 95),

-- ============ ADDITIONAL AFRICAN (5) ============
('00000000-0000-0000-0000-000000000001', 'ZA', 'ZAF', '710', 'south-africa', 'South Africa', 'جنوب أفريقيا', 'South African', 'جنوب أفريقي', '+27', 'ZAR', 'South African Rand', 'راند جنوب أفريقي', '🇿🇦', 'Pretoria', 'بريتوريا', 'Sub-Saharan Africa', 'Southern Africa', FALSE, FALSE, FALSE, FALSE, 'STANDARD', FALSE, NULL, TRUE, 80),
('00000000-0000-0000-0000-000000000001', 'KE', 'KEN', '404', 'kenya', 'Kenya', 'كينيا', 'Kenyan', 'كيني', '+254', 'KES', 'Kenyan Shilling', 'شلن كيني', '🇰🇪', 'Nairobi', 'نيروبي', 'Sub-Saharan Africa', 'East Africa', FALSE, FALSE, TRUE, FALSE, 'STANDARD', FALSE, NULL, TRUE, 81),
('00000000-0000-0000-0000-000000000001', 'GH', 'GHA', '288', 'ghana', 'Ghana', 'غانا', 'Ghanaian', 'غاني', '+233', 'GHS', 'Ghanaian Cedi', 'سيدي غاني', '🇬🇭', 'Accra', 'أكرا', 'Sub-Saharan Africa', 'West Africa', FALSE, FALSE, FALSE, FALSE, 'STANDARD', FALSE, NULL, TRUE, 82),
('00000000-0000-0000-0000-000000000001', 'TZ', 'TZA', '834', 'tanzania', 'Tanzania', 'تنزانيا', 'Tanzanian', 'تنزاني', '+255', 'TZS', 'Tanzanian Shilling', 'شلن تنزاني', '🇹🇿', 'Dodoma', 'دودوما', 'Sub-Saharan Africa', 'East Africa', FALSE, FALSE, TRUE, FALSE, 'STANDARD', FALSE, NULL, TRUE, 83),
('00000000-0000-0000-0000-000000000001', 'UG', 'UGA', '800', 'uganda', 'Uganda', 'أوغندا', 'Ugandan', 'أوغندي', '+256', 'UGX', 'Ugandan Shilling', 'شلن أوغندي', '🇺🇬', 'Kampala', 'كمبالا', 'Sub-Saharan Africa', 'East Africa', FALSE, FALSE, TRUE, FALSE, 'STANDARD', FALSE, NULL, TRUE, 84),

-- ============ ADDITIONAL EUROPEAN (5) ============
('00000000-0000-0000-0000-000000000001', 'ES', 'ESP', '724', 'spain', 'Spain', 'إسبانيا', 'Spanish', 'إسباني', '+34', 'EUR', 'Euro', 'يورو', '🇪🇸', 'Madrid', 'مدريد', 'Europe', 'Southern Europe', FALSE, FALSE, FALSE, FALSE, 'LOW', TRUE, 24, TRUE, 67),
('00000000-0000-0000-0000-000000000001', 'SE', 'SWE', '752', 'sweden', 'Sweden', 'السويد', 'Swedish', 'سويدي', '+46', 'SEK', 'Swedish Krona', 'كرونة سويدية', '🇸🇪', 'Stockholm', 'ستوكهولم', 'Europe', 'Northern Europe', FALSE, FALSE, FALSE, FALSE, 'LOW', TRUE, 24, TRUE, 68),
('00000000-0000-0000-0000-000000000001', 'NO', 'NOR', '578', 'norway', 'Norway', 'النرويج', 'Norwegian', 'نرويجي', '+47', 'NOK', 'Norwegian Krone', 'كرونة نرويجية', '🇳🇴', 'Oslo', 'أوسلو', 'Europe', 'Northern Europe', FALSE, FALSE, FALSE, FALSE, 'LOW', TRUE, 15, TRUE, 69),
('00000000-0000-0000-0000-000000000001', 'AT', 'AUT', '040', 'austria', 'Austria', 'النمسا', 'Austrian', 'نمساوي', '+43', 'EUR', 'Euro', 'يورو', '🇦🇹', 'Vienna', 'فيينا', 'Europe', 'Western Europe', FALSE, FALSE, FALSE, FALSE, 'LOW', TRUE, 20, TRUE, 100),
('00000000-0000-0000-0000-000000000001', 'IE', 'IRL', '372', 'ireland', 'Ireland', 'أيرلندا', 'Irish', 'أيرلندي', '+353', 'EUR', 'Euro', 'يورو', '🇮🇪', 'Dublin', 'دبلن', 'Europe', 'Northern Europe', FALSE, FALSE, FALSE, FALSE, 'LOW', TRUE, 22, TRUE, 101),

-- ============ ADDITIONAL ASIAN (3) ============
('00000000-0000-0000-0000-000000000001', 'TH', 'THA', '764', 'thailand', 'Thailand', 'تايلاند', 'Thai', 'تايلاندي', '+66', 'THB', 'Thai Baht', 'بات تايلاندي', '🇹🇭', 'Bangkok', 'بانكوك', 'Southeast Asia', 'South-Eastern Asia', FALSE, FALSE, TRUE, FALSE, 'STANDARD', FALSE, NULL, TRUE, 85),
('00000000-0000-0000-0000-000000000001', 'VN', 'VNM', '704', 'vietnam', 'Vietnam', 'فيتنام', 'Vietnamese', 'فيتنامي', '+84', 'VND', 'Vietnamese Dong', 'دونغ فيتنامي', '🇻🇳', 'Hanoi', 'هانوي', 'Southeast Asia', 'South-Eastern Asia', FALSE, FALSE, FALSE, FALSE, 'STANDARD', FALSE, NULL, TRUE, 86),
('00000000-0000-0000-0000-000000000001', 'NZ', 'NZL', '554', 'new-zealand', 'New Zealand', 'نيوزيلندا', 'New Zealander', 'نيوزيلندي', '+64', 'NZD', 'New Zealand Dollar', 'دولار نيوزيلندي', '🇳🇿', 'Wellington', 'ويلينغتون', 'Oceania', 'Australasia', FALSE, FALSE, FALSE, FALSE, 'LOW', FALSE, NULL, TRUE, 87);

-- =============================================
-- 4. UPDATE EXISTING DATA (set slug for any that lack it)
-- =============================================
UPDATE countries SET slug = LOWER(REPLACE(REPLACE(name_en, ' ', '-'), '''', '')) WHERE slug IS NULL;
