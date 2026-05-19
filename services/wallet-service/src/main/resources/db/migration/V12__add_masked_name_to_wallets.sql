-- V12: Freeze the recipient display name on the wallet row.
--
-- Until now each lookup (verify-recipient, recent recipients, transaction
-- history) re-computed a masked name from identity-service on the fly, which
-- meant the same wallet could show different fallback names across calls.
--
-- We now capture the name once at wallet-creation time (sourced from the
-- Nafath mock pool via identity-service) and reuse it for every subsequent
-- read. Existing rows are back-filled from the 100-entry pool so the demo
-- stays coherent.

ALTER TABLE wallets ADD COLUMN IF NOT EXISTS masked_name VARCHAR(120);

UPDATE wallets
   SET masked_name = (ARRAY[
        'Usman Arshad','Mirza Momin Baig','Hussain Shah','Muhammad Ehsaan',
        'Arslan Kibriya','Umair Asim','Muzaffar Luqman','Ahmed Al-Saud',
        'Khalid Al-Otaibi','Faisal Al-Harbi','Omar Al-Ghamdi','Saud Al-Qahtani',
        'Yasir Al-Dossari','Bandar Al-Shehri','Nawaf Al-Mutairi','Turki Al-Anazi',
        'Majed Al-Zahrani','Hassan Al-Maliki','Salman Al-Subaie','Abdullah Al-Sahli',
        'Mohammed Al-Rashidi','Fahad Al-Balawi','Sultan Al-Juhani','Waleed Al-Asmari',
        'Rakan Al-Shamrani','Ibrahim Al-Hazmi','Tariq Al-Qarni','Rayan Al-Otaibi',
        'Ziyad Al-Faraj','Mansour Al-Amri','Ali Hassan','Yousef Khan',
        'Bilal Rehman','Asad Mahmood','Imran Siddiqui','Zubair Tariq',
        'Faraz Akhtar','Hamza Younis','Saif Iqbal','Adeel Nawaz',
        'Junaid Farooq','Kashif Anwar','Mubeen Sultan','Nadeem Pasha',
        'Owais Mehmood','Qasim Raza','Rehan Aslam','Sameer Hashmi',
        'Talha Mansoor','Usama Naseer','Wasim Akram','Zain Mehdi',
        'Aamir Sohail','Basit Ali','Danish Kaneria','Ehtisham Butt',
        'Fawad Alam','Ghulam Mustafa','Haris Sohail','Inzamam Haq',
        'Jamshed Khan','Kamran Khan','Lateef Ahmad','Misbah Ahmed',
        'Nasir Jamshed','Owais Shah','Pervez Iqbal','Qadir Mohammed',
        'Raheel Sharif','Sarfaraz Khan','Tahir Naveed','Umar Akmal',
        'Veer Singh','Waqar Younis','Xavier Marshall','Yasin Malik',
        'Zaheer Abbas','Adnan Khan','Bashir Ahmad','Chand Pasha',
        'Daud Ibrahim','Eshaan Verma','Furqan Qureshi','Ghazi Khan',
        'Hadi Matar','Irfan Pathan','Jameel Ahmad','Kabir Khan',
        'Liaqat Ali','Mehboob Khan','Nashit Hussain','Owais Qadri',
        'Parvez Musharraf','Qaiser Abbas','Rashid Latif','Saqlain Mushtaq',
        'Tanveer Ahmad','Usman Khawaja','Vasim Bari','Wahab Riaz',
        'Yawar Saeed','Zulfiqar Ali'
   ])[floor(random() * 100 + 1)::int]
 WHERE masked_name IS NULL;
