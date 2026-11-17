#!/usr/bin/env python3
"""One-off generator for app/src/main/assets/syllabus.json (kept outside the repo; the JSON is the source of truth).

Sources (official TNPSC documents):
  G1 prelims: code 497, dated 16.12.2024            (tnpsc.gov.in/static_pdf/syllabus/497_group 1 preliminary syllabus.pdf)
  G2/2A     : scheme & syllabus revised 27.01.2022  (tnpsc.gov.in/static_pdf/syllabus/G2_Revised_Scheme_Syllabus_27012022.pdf)
  G4/VAO    : scheme & syllabus revised 27.01.2022  (tnpsc.gov.in/static_pdf/syllabus/G4_Scheme_Revised_27012022.pdf)
Exam digits: 1=G1, 2=G2, 4=G4  ("124" = shared by all three).
"""
import json, sys

CODE = {"1": "G1", "2": "G2", "4": "G4"}

def T(en, ta, ex):
    return (en, ta, ex)

SUBJECTS = []

def subject(sid, en, ta, units):
    SUBJECTS.append((sid, en, ta, units))

# ---------------------------------------------------------------- General Science
subject("sci", "General Science", "பொது அறிவியல்", [
    ("Physics", "இயற்பியல்", [
        T("Scientific knowledge and scientific temper; power of reasoning", "அறிவியல் அறிவு மற்றும் அறிவியல் உணர்வு; பகுத்தறிதல்", "12"),
        T("Rote learning vs conceptual learning; science as a tool to understand past, present and future", "பொருள் உணராமல் கற்றலும் கருத்துணர்ந்து கற்றலும்; கடந்தகாலம், நிகழ்காலம், எதிர்காலம் பற்றி புரிந்து கொள்வதற்கான ஒரு கருவி அறிவியல்", "12"),
        T("Nature of the universe", "பேரண்டத்தின் இயல்பு", "124"),
        T("Measurement of physical quantities", "இயற்பியல் அளவுகளின் அளவீடுகள்", "4"),
        T("General scientific laws: mechanics, properties of matter, force, motion and energy", "பொது அறிவியல் விதிகள் - இயக்கவியல் - பருப்பொருளின் பண்புகள், விசை, இயக்கம் மற்றும் ஆற்றல்", "124"),
        T("Everyday applications of mechanics, electricity, magnetism, light, sound, heat and nuclear physics", "அன்றாட வாழ்வில் இயக்கவியல், மின்னியல், காந்தவியல், ஒளி, ஒலி, வெப்பம், அணுக்கரு இயற்பியல் ஆகியவற்றின் அடிப்படை கோட்பாடுகளின் பயன்பாடுகள்", "124"),
        T("Laser, electronics and communications", "லேசர் (LASER), மின்னணுவியல் மற்றும் தகவல் தொடர்பியல்", "12"),
    ]),
    ("Chemistry", "வேதியியல்", [
        T("Elements and compounds", "தனிமங்களும் சேர்மங்களும்", "124"),
        T("Acids, bases and salts", "அமிலங்கள், காரங்கள், உப்புகள்", "124"),
        T("Petroleum products", "பெட்ரோலியப் பொருட்கள்", "124"),
        T("Fertilizers and pesticides", "உரங்கள், பூச்சிகொல்லிகள்", "124"),
        T("Metallurgy and food adulterants", "உலோகவியல் மற்றும் உணவில் கலப்படம்", "4"),
    ]),
    ("Life science", "உயிரியல்", [
        T("Main concepts of life science", "உயிரியலின் முக்கிய கோட்பாடுகள்", "124"),
        T("Classification of living organisms", "உயிர் உலகின் வகைப்பாடு", "124"),
        T("Evolution and genetics", "பரிணாமம், மரபியல்", "124"),
        T("Physiology", "உடலியங்கியல்", "124"),
        T("Nutrition, health and hygiene", "உணவியல், உடல்நலம் மற்றும் சுகாதாரம்", "124"),
        T("Human diseases", "மனிதநோய்கள்", "124"),
    ]),
    ("Environment", "சுற்றுச்சூழல்", [
        T("Environment and ecology", "சுற்றுப்புறச்சூழல் மற்றும் சூழலியல்", "124"),
    ]),
])

# ---------------------------------------------------------------- Geography of India
subject("geo", "Geography of India", "இந்தியாவின் புவியியல்", [
    ("Physical geography", "இயற்கைப் புவியியல்", [
        T("Location", "அமைவிடம்", "124"),
        T("Physical features", "இயற்கை அமைவுகள்", "124"),
        T("Monsoon, rainfall, weather and climate", "பருவமழை, மழைப்பொழிவு, வானிலை மற்றும் காலநிலை", "124"),
        T("Water resources", "நீர் வளங்கள்", "124"),
        T("Rivers in India", "இந்திய ஆறுகள்", "124"),
        T("Soil, minerals and natural resources", "மண், கனிம வளங்கள் மற்றும் இயற்கை வளங்கள்", "124"),
        T("Forest and wildlife", "காடு மற்றும் வன உயிரினங்கள்", "124"),
        T("Agricultural pattern", "வேளாண் முறைகள்", "124"),
    ]),
    ("Transport, communication and social geography", "போக்குவரத்து, தகவல் தொடர்பு மற்றும் சமூகப் புவியியல்", [
        T("Transport", "போக்குவரத்து", "124"),
        T("Communication", "தகவல் தொடர்பு", "124"),
        T("Population density and distribution", "மக்கள்தொகை அடர்த்தி மற்றும் பரவல்", "124"),
        T("Racial and linguistic groups; major tribes", "இனம், மொழிக் குழுக்கள் மற்றும் முக்கியப் பழங்குடிகள்", "12"),
    ]),
    ("Disasters and environment", "பேரிடர் மற்றும் சுற்றுச்சூழல்", [
        T("Natural calamities and disaster management", "இயற்கைப் பேரிடர் - பேரிடர் மேலாண்மை", "124"),
        T("Environmental pollution: causes and preventive measures", "சுற்றுச்சூழல் மாசுபடுதல் - காரணங்களும் தடுப்பு முறைகளும்", "12"),
        T("Climate change", "பருவநிலை மாற்றம்", "124"),
        T("Green energy", "பசுமை ஆற்றல்", "12"),
    ]),
])

# ---------------------------------------------------------------- History and Culture of India
subject("hist", "History and Culture of India", "இந்தியாவின் வரலாறு மற்றும் பண்பாடு", [
    ("Ancient and medieval India", "பண்டைய மற்றும் இடைக்கால இந்தியா", [
        T("Indus Valley civilization", "சிந்து சமவெளி நாகரிகம்", "124"),
        T("Guptas", "குப்தர்கள்", "124"),
        T("Delhi Sultans", "தில்லி சுல்தான்கள்", "124"),
        T("Mughals", "முகலாயர்கள்", "124"),
        T("Marathas", "மராத்தியர்கள்", "124"),
        T("Age of Vijayanagara and Bahmani kingdoms", "விஜயநகர மற்றும் பாமினி அரசுகளின் காலம்", "12"),
        T("South Indian history", "தென் இந்திய வரலாறு", "124"),
    ]),
    ("Indian culture", "இந்தியப் பண்பாடு", [
        T("Change and continuity in the socio-cultural history of India", "இந்திய சமூகப் பண்பாட்டு வரலாற்றில் மாற்றங்களும் தொடர்ச்சியும்", "12"),
        T("Characteristics of Indian culture; unity in diversity (race, language, custom)", "இந்தியப் பண்பாட்டின் இயல்புகள், வேற்றுமையில் ஒற்றுமை - இனம், மொழி, வழக்காறு", "124"),
        T("India as a secular state", "இந்தியா ஒரு மதச்சார்பற்ற நாடு", "124"),
        T("Social harmony", "சமூக நல்லிணக்கம்", "12"),
    ]),
])

# ---------------------------------------------------------------- Indian National Movement
subject("nm", "Indian National Movement", "இந்திய தேசிய இயக்கம்", [
    ("Freedom struggle", "விடுதலைப் போராட்டம்", [
        T("National renaissance", "தேசிய மறுமலர்ச்சி", "124"),
        T("Early uprisings against British rule", "ஆங்கிலேயர் ஆட்சிக்கு எதிரான தொடக்க கால எழுச்சிகள்", "124"),
        T("Indian National Congress", "இந்திய தேசிய காங்கிரஸ்", "124"),
        T("Emergence of leaders (Ambedkar, Bhagat Singh, Bharathiar, V.O.Chidambaranar, Nehru, Kamarajar, Gandhi, Azad, Periyar, Rajaji, Bose, Tagore and others)", "தலைவர்கள் உருவாதல் (அம்பேத்கர், பகத்சிங், பாரதியார், வ.உ.சிதம்பரனார், ஜவகர்லால் நேரு, காமராசர், மகாத்மா காந்தி, மௌலானா அபுல் கலாம் ஆசாத், தந்தை பெரியார், இராஜாஜி, சுபாஷ் சந்திர போஸ், ரவீந்திரநாத் தாகூர் மற்றும் பலர்)", "124"),
        T("Different modes of agitation: growth of Satyagraha and militant movements", "விடுதலைப் போராட்டத்தின் பல்வேறு நிலைகள்: அகிம்சை முறையின் வளர்ச்சி மற்றும் புரட்சிகர இயக்கங்கள்", "12"),
        T("Modes of agitation of Tamil Nadu and movements", "தமிழ்நாட்டின் போராட்ட வடிவங்களும் இயக்கங்களும்", "4"),
        T("Communalism and Partition", "வகுப்புவாதம் மற்றும் தேசப் பிரிவினை", "12"),
    ]),
])

# ---------------------------------------------------------------- Indian Polity
subject("pol", "Indian Polity", "இந்திய ஆட்சியியல்", [
    ("The Constitution", "அரசியலமைப்பு", [
        T("Constitution of India", "இந்திய அரசியலமைப்பு", "124"),
        T("Preamble to the Constitution", "அரசியலமைப்பின் முகவுரை", "124"),
        T("Salient features of the Constitution", "அரசியலமைப்பின் முக்கிய கூறுகள்", "124"),
        T("Union, State and Union Territory", "ஒன்றியம், மாநிலம் மற்றும் யூனியன் பிரதேசங்கள்", "124"),
        T("Citizenship", "குடியுரிமை", "124"),
        T("Fundamental Rights", "அடிப்படை உரிமைகள்", "124"),
        T("Fundamental Duties", "அடிப்படைக் கடமைகள்", "124"),
        T("Directive Principles of State Policy", "அரசின் நெறிமுறைக் கோட்பாடுகள்", "124"),
    ]),
    ("Government and federalism", "அரசாங்கம் மற்றும் கூட்டாட்சி", [
        T("Union Executive", "ஒன்றிய நிர்வாகம்", "124"),
        T("Union Legislature", "ஒன்றிய நாடாளுமன்றம்", "124"),
        T("State Executive", "மாநில நிர்வாகம்", "124"),
        T("State Legislature", "மாநில சட்டமன்றம்", "124"),
        T("Local governments; Panchayat Raj", "உள்ளாட்சி அமைப்புகள், பஞ்சாயத்து ராஜ்", "124"),
        T("Spirit of federalism; Centre-State relationships", "கூட்டாட்சியின் அடிப்படைத் தன்மைகள்; மத்திய - மாநில உறவுகள்", "124"),
        T("Election", "தேர்தல்", "124"),
        T("Judiciary in India", "இந்திய நீதி அமைப்புகள்", "124"),
        T("Rule of law", "சட்டத்தின் ஆட்சி", "124"),
    ]),
    ("Accountability and rights", "பொறுப்புடைமை மற்றும் உரிமைகள்", [
        T("Corruption in public life; anti-corruption measures", "பொதுவாழ்வில் ஊழல் - ஊழல் தடுப்பு நடவடிக்கைகள்", "124"),
        T("Lokpal and Lokayukta", "லோக்பால் மற்றும் லோக் ஆயுக்தா", "124"),
        T("Right to Information", "தகவல் உரிமை", "124"),
        T("Empowerment of women", "பெண்களுக்கு அதிகாரமளித்தல்", "124"),
        T("Consumer protection forums", "நுகர்வோர் பாதுகாப்பு அமைப்புகள்", "124"),
        T("Human rights charter", "மனித உரிமைகள் சாசனம்", "124"),
    ]),
])

# ---------------------------------------------------------------- Indian Economy
subject("eco", "Indian Economy", "இந்தியப் பொருளாதாரம்", [
    ("Planning and public finance", "திட்டமிடலும் பொது நிதியும்", [
        T("Nature of the Indian economy", "இந்தியப் பொருளாதாரத்தின் இயல்புகள்", "124"),
        T("Five-year plan models: an assessment", "ஐந்தாண்டு திட்ட மாதிரிகள் - ஒரு மதிப்பீடு", "124"),
        T("Planning Commission and NITI Aayog", "திட்டக்குழு மற்றும் நிதி ஆயோக்", "124"),
        T("Sources of revenue", "வருவாய் ஆதாரங்கள்", "124"),
        T("Reserve Bank of India", "இந்திய ரிசர்வ் வங்கி", "124"),
        T("Fiscal policy and monetary policy", "நிதி கொள்கை மற்றும் பணவியல் கொள்கை", "12"),
        T("Finance Commission", "நிதி ஆணையம்", "124"),
        T("Resource sharing between Union and State governments", "மத்திய மாநில அரசுகளுக்கிடையேயான நிதிப் பகிர்வு", "124"),
        T("Goods and Services Tax", "சரக்கு மற்றும் சேவை வரி", "124"),
    ]),
    ("Economy, agriculture and society", "பொருளாதாரம், வேளாண்மை மற்றும் சமூகம்", [
        T("Structure of the Indian economy", "இந்தியப் பொருளாதார அமைப்பு", "12"),
        T("Economic trends", "பொருளாதாரப் போக்குகள்", "4"),
        T("Employment generation", "வேலைவாய்ப்பு உருவாக்கம்", "124"),
        T("Land reforms and agriculture", "நிலச் சீர்திருத்தங்கள் மற்றும் வேளாண்மை", "124"),
        T("Application of science and technology in agriculture", "வேளாண்மையில் அறிவியல் தொழில்நுட்பத்தின் பயன்பாடு", "124"),
        T("Industrial growth", "தொழில் வளர்ச்சி", "124"),
        T("Rural welfare oriented programmes", "ஊரக நலன்சார் திட்டங்கள்", "124"),
        T("Social problems: population, education, health, employment, poverty", "சமூகப் பிரச்சனைகள் - மக்கள்தொகை, கல்வி, நலவாழ்வு, வேலைவாய்ப்பு, வறுமை", "124"),
    ]),
])

# ---------------------------------------------------------------- Tamil Nadu: history, culture, movements
subject("tn-hist", "History, Culture, Heritage and Socio-Political Movements of Tamil Nadu", "தமிழ்நாட்டின் வரலாறு, பண்பாடு, மரபு மற்றும் சமூக - அரசியல் இயக்கங்கள்", [
    ("Tamil society and literature", "தமிழ்ச் சமுதாயமும் இலக்கியமும்", [
        T("History of Tamil society and related archaeological discoveries", "தமிழ் சமுதாய வரலாறு, அது தொடர்பான தொல்லியல் கண்டுபிடிப்புகள்", "124"),
        T("Tamil literature from the Sangam age till contemporary times", "சங்க காலம் முதல் இக்காலம் வரையிலான தமிழ் இலக்கிய வரலாறு", "124"),
    ]),
    ("Thirukkural", "திருக்குறள்", [
        T("Significance as a secular literature", "மதச் சார்பற்ற தனித்தன்மையுள்ள இலக்கியம்", "124"),
        T("Relevance to everyday life", "அன்றாட வாழ்வியலோடு தொடர்புத் தன்மை", "124"),
        T("Impact of Thirukkural on humanity", "மானுடத்தின் மீதான திருக்குறளின் தாக்கம்", "124"),
        T("Thirukkural and universal values (equality, humanism, etc.)", "திருக்குறளும் மாறாத விழுமியங்களும்", "124"),
        T("Relevance to socio-politico-economic affairs", "சமூக அரசியல் பொருளாதார நிகழ்வுகளில் திருக்குறளின் பொருத்தப்பாடு", "124"),
        T("Philosophical content in Thirukkural", "திருக்குறளில் தத்துவக் கோட்பாடுகள்", "124"),
    ]),
    ("Freedom struggle and social movements in Tamil Nadu", "தமிழ்நாட்டில் விடுதலைப் போராட்டமும் சமூக இயக்கங்களும்", [
        T("Role of Tamil Nadu in the freedom struggle", "விடுதலைப் போராட்டத்தில் தமிழ்நாட்டின் பங்கு", "124"),
        T("Early agitations against British rule", "ஆங்கிலேயருக்கு எதிரான தொடக்க கால கிளர்ச்சிகள்", "124"),
        T("Role of women in the freedom struggle", "விடுதலைப் போராட்டத்தில் பெண்களின் பங்கு", "124"),
        T("19th and 20th century socio-political movements in Tamil Nadu: Justice Party, growth of rationalism", "பத்தொன்பதாம் மற்றும் இருபதாம் நூற்றாண்டுகளில் தமிழ்நாட்டின் சமூக - அரசியல் இயக்கங்களின் பரிணாம வளர்ச்சி - நீதிக்கட்சி, பகுத்தறிவு வாதத்தின் வளர்ச்சி", "12"),
        T("Self-Respect Movement and Dravidian Movement; their underlying principles", "சுயமரியாதை இயக்கம், திராவிட இயக்கம் மற்றும் இவ்வியக்கங்களுக்கான அடிப்படைக் கொள்கைகள்", "12"),
        T("Contributions of Thanthai Periyar and Perarignar Anna", "தந்தை பெரியார் மற்றும் பேரறிஞர் அண்ணாவின் பங்களிப்புகள்", "12"),
        T("Social reformers, social reform movements and social transformation of Tamil Nadu", "சமூக சீர்திருத்தவாதிகள், சமூக சீர்திருத்த இயக்கங்கள் மற்றும் தமிழ்நாட்டின் சமூக மாற்றம்", "4"),
    ]),
])

# ---------------------------------------------------------------- Development administration in Tamil Nadu
subject("tn-dev", "Development Administration in Tamil Nadu", "தமிழ்நாட்டில் வளர்ச்சி நிர்வாகம்", [
    ("Development administration", "வளர்ச்சி நிர்வாகம்", [
        T("Human Development Indicators in Tamil Nadu and a comparison across the country", "தமிழ்நாட்டின் மனிதவள மேம்பாட்டுக் குறியீடுகளும் அவற்றை தேசிய மற்றும் பிற மாநிலங்களுக்கான குறியீடுகளுடன் ஒப்பாய்வும்", "12"),
        T("Impact of social reform movements on the socio-economic development of Tamil Nadu", "தமிழ்நாட்டின் சமூக பொருளாதார வளர்ச்சிக்கு சமூக மறுமலர்ச்சி இயக்கங்களின் பங்களிப்பு", "12"),
        T("Political parties and welfare schemes for various sections of people", "அரசியல் கட்சிகளும் பலதரப்பு மக்களுக்கான நலத்திட்டங்களும்", "12"),
        T("Rationale behind the reservation policy; access to social resources", "இடஒதுக்கீட்டுக் கொள்கைக்கான நியாயங்களும் சமூக வளங்களைப் பெறும் வாய்ப்புகளும்", "12"),
        T("Economic trends in Tamil Nadu", "தமிழ்நாட்டின் பொருளாதார போக்குகள்", "12"),
        T("Role and impact of social welfare schemes in the socio-economic development of Tamil Nadu", "தமிழ்நாட்டின் சமூக பொருளாதார வளர்ச்சியில் சமூகநலத் திட்டங்களின் தாக்கமும் பங்களிப்பும்", "12"),
        T("Social justice and social harmony as cornerstones of socio-economic development", "சமூக நீதியும் சமூக நல்லிணக்கமும் சமூகப் பொருளாதார மேம்பாட்டின் மூலாதாரங்கள்", "124"),
        T("Education and health systems in Tamil Nadu", "தமிழ்நாட்டின் கல்வி மற்றும் நலவாழ்வு (Health) முறைமைகள்", "124"),
        T("Geography of Tamil Nadu and its impact on economic growth", "தமிழ்நாட்டின் புவியியல் கூறுகளும் பொருளாதார வளர்ச்சியில் அவற்றின் தாக்கமும்", "124"),
        T("Achievements of Tamil Nadu in various fields", "பல்வேறு துறைகளில் தமிழ்நாடு நிகழ்த்தியுள்ள சாதனைகள்", "12"),
        T("e-Governance in Tamil Nadu", "தமிழ்நாட்டில் மின்னாளுகை", "12"),
    ]),
])

# ---------------------------------------------------------------- Current events
subject("cur", "Current Events", "நடப்பு நிகழ்வுகள்", [
    ("Current events", "நடப்பு நிகழ்வுகள்", [
        T("Current affairs (Group I lists this under every unit)", "நடப்பு நிகழ்வுகள் (தொகுதி I-இல் ஒவ்வொரு அலகிலும் இடம்பெறுகிறது)", "1"),
        T("Latest diary of events", "அண்மை நிகழ்வுகளின் தொகுப்பு", "24"),
        T("National symbols", "தேசிய சின்னங்கள்", "124"),
        T("Profile of states", "மாநிலங்கள் குறித்த விவரங்கள்", "24"),
        T("Eminent personalities and places in news", "செய்திகளில் இடம்பெற்ற சிறந்த ஆளுமைகளும் இடங்களும்", "124"),
        T("Sports", "விளையாட்டு", "124"),
        T("Books and authors", "நூல்களும் ஆசிரியர்களும்", "124"),
        T("Prominent personalities in arts, science, literature and philosophy", "கலை, அறிவியல், இலக்கியம் மற்றும் தத்துவம் ஆகிய வெவ்வேறு துறைகளில் தனித்துவம் கொண்ட ஆளுமைகள்", "12"),
        T("Latest inventions in science and technology", "அறிவியல் மற்றும் தொழில்நுட்பத்தில் அண்மைக்கால கண்டுபிடிப்புகள்", "124"),
        T("Geographical landmarks", "புவியியல் அடையாளங்கள்", "124"),
        T("Current socio-economic issues", "தற்போதைய சமூகப் பொருளாதார பிரச்சனைகள்", "124"),
        T("Political parties and political system", "அரசியல் கட்சிகளும் ஆட்சியியல் முறைகளும்", "124"),
        T("Welfare-oriented government schemes and their utility", "நலன்சார் அரசுத் திட்டங்களும் அவற்றின் பயன்பாடும்", "124"),
        T("Public awareness, general administration and problems in public delivery systems", "பொது விழிப்புணர்வும் பொது நிர்வாகமும்; பொது விநியோக அமைப்புகளில் நிலவும் சிக்கல்கள்", "12"),
    ]),
])

# ---------------------------------------------------------------- Aptitude and Mental Ability
subject("apt", "Aptitude and Mental Ability", "திறனறிவும் மனக்கணக்கு நுண்ணறிவும்", [
    ("Aptitude", "திறனறிவு", [
        T("Simplification", "சுருக்குதல்", "124"),
        T("Percentage", "விழுக்காடு", "124"),
        T("Highest Common Factor (HCF)", "மீப்பெரு பொதுக் காரணி (HCF)", "124"),
        T("Lowest Common Multiple (LCM)", "மீச்சிறு பொது மடங்கு (LCM)", "124"),
        T("Ratio and proportion", "விகிதம் மற்றும் விகிதாச்சாரம்", "124"),
        T("Simple interest", "தனிவட்டி", "124"),
        T("Compound interest", "கூட்டு வட்டி", "124"),
        T("Area", "பரப்பு", "124"),
        T("Volume", "கொள்ளளவு", "124"),
        T("Time and work", "காலம் மற்றும் வேலை", "124"),
    ]),
    ("Reasoning", "காரணவியல்", [
        T("Logical reasoning", "தருக்கக் காரணவியல்", "124"),
        T("Puzzles", "புதிர்கள்", "124"),
        T("Dice", "பகடை", "124"),
        T("Visual reasoning", "காட்சிக் காரணவியல்", "124"),
        T("Alpha-numeric reasoning", "எண் எழுத்துக் காரணவியல்", "124"),
        T("Number series", "எண் வரிசை", "124"),
    ]),
])

# ---------------------------------------------------------------- General Tamil (SSLC standard) -- G2 (alternative to English) and G4
subject("tamil", "General Tamil (SSLC standard)", "பொதுத்தமிழ் (பத்தாம் வகுப்புத் தரம்)", [
    ("Part A: Grammar", "பகுதி அ: இலக்கணம்", [
        T("Matching: meanings, famous works and authors", "பொருத்துதல் – பொருத்தமான பொருளைத் தேர்வு செய்தல், புகழ் பெற்ற நூல், நூலாசிரியர்", "24"),
        T("Identify the person or work referred to by a phrase or epithet", "தொடரும் தொடர்பும் அறிதல் (இத்தொடரால் குறிக்கப்படும் சான்றோர்; அடைமொழியால் குறிக்கப்படும் நூல்)", "24"),
        T("Split the word (separation)", "பிரித்தெழுதுக", "24"),
        T("Antonyms", "எதிர்ச்சொல்லை எடுத்தெழுதுதல்", "24"),
        T("Odd word out", "பொருந்தாச் சொல்லைக் கண்டறிதல்", "24"),
        T("Error correction: sandhi, number, idiom, vazhu, foreign words", "பிழைதிருத்தம் - சந்திப்பிழையை நீக்குதல், ஒருமை பன்மை பிழைகளை நீக்குதல், மரபுப் பிழைகள், வழுஉச் சொற்களை நீக்குதல், பிறமொழிச் சொற்களை நீக்குதல்", "24"),
        T("Tamil equivalent of an English word", "ஆங்கிலச் சொல்லுக்கு நேரான தமிழ்ச் சொல்லை அறிதல்", "24"),
        T("Sound differences and the correct meaning", "ஒலி வேறுபாடறிந்து சரியான பொருளை அறிதல்", "24"),
        T("One-letter words and their meanings", "ஓரெழுத்து ஒரு மொழி உரிய பொருளைக் கண்டறிதல்", "24"),
        T("Choose the root word", "வேர்ச்சொல்லைத் தேர்வு செய்தல்", "24"),
        T("Form verb and noun forms from a root word", "வேர்ச்சொல்லைக் கொடுத்து, வினைமுற்று, வினையெச்சம், வினையாலணையும் பெயர், தொழிற்பெயரை உருவாக்கல்", "24"),
        T("Arrange words in alphabetical order", "அகர வரிசைப்படி சொற்களைச் சீர்செய்தல்", "24"),
        T("Arrange words into a sentence", "சொற்களை ஒழுங்குபடுத்தி சொற்றொடர் ஆக்குதல்", "24"),
        T("Identify the type of noun", "பெயர்ச் சொல்லின் வகை அறிதல்", "24"),
        T("Grammatical notes", "இலக்கணக் குறிப்பறிதல்", "24"),
        T("Choose the question that fits the answer", "விடைக்கேற்ற வினாவைத் தேர்ந்தெடுத்தல்", "24"),
        T("Identify the type of sentence", "எவ்வகை வாக்கியம் எனக் கண்டெழுதுதல்", "24"),
        T("Active, passive, transitive and causative sentences", "தன்வினை, பிறவினை, செய்வினை, செயப்பாட்டுவினை வாக்கியங்களைக் கண்டெழுதுதல்", "24"),
        T("Meaning explained by a simile", "உவமையால் விளக்கப்பெறும் பொருத்தமான பொருளைத் தேர்ந்தெழுதுதல்", "24"),
        T("Prosody: ethugai, monai, iyaipu", "எதுகை, மோனை, இயைபு இவற்றுள் ஏதேனும் ஒன்றை தேர்ந்தெழுதுதல்", "24"),
        T("Proverbs", "பழமொழிகள்", "24"),
    ]),
    ("Part B: Literature", "பகுதி ஆ: இலக்கியம்", [
        T("Thirukkural: news, quotations, completing the line (25 chapters only)", "திருக்குறள் தொடர்பான செய்திகள், மேற்கோள்கள், தொடரை நிரப்புதல் (இருபத்தைந்து அதிகாரம் மட்டும்)", "24"),
        T("Didactic works: Naaladiyar, Naanmanikkadigai, Pazhamozhi Naanooru, Mudhumozhikkanji, Thirikadugam, Innaa Naarpadhu, Iniyavai Naarpadhu, Sirupanchamoolam, Elaadhi, Avvaiyar's verses, Pathinenkeezhkanakku", "அறநூல்கள் – நாலடியார், நான்மணிக்கடிகை, பழமொழி நானூறு, முதுமொழிக் காஞ்சி, திரிகடுகம், இன்னா நாற்பது, இனியவை நாற்பது, சிறுபஞ்சமூலம், ஏலாதி, ஔவையார் பாடல்கள் தொடர்பான செய்திகள், பதினெண்கீழ்க்கணக்கு நூல்களில் பிற செய்திகள்", "24"),
        T("Kambaramayanam and Ravana Kaviyam; verse forms; notable lines", "கம்பராமாயணம், இராவண காவியம் தொடர்பான செய்திகள், பாவகை, சிறந்த தொடர்கள்", "24"),
        T("Sangam literature: Purananooru, Aganaanooru, Natrinai, Kurunthogai, Ainkurunooru, Kalithogai, Eight Anthologies, Ten Idylls", "புறநானூறு, அகநானூறு, நற்றிணை, குறுந்தொகை, ஐங்குறுநூறு, கலித்தொகை தொடர்பான செய்திகள், மேற்கோள்கள், அடியரையறை, எட்டுத்தொகை, பத்துப்பாட்டு நூல்களில் உள்ள பிற செய்திகள்", "24"),
        T("Silappathikaram, Manimekalai and the five great and five minor epics", "சிலப்பதிகாரம்-மணிமேகலை தொடர்பான செய்திகள், மேற்கோள்கள், சிறந்த தொடர்கள், உட்பிரிவுகள் மற்றும் ஐம்பெரும்-ஐஞ்சிறுங் காப்பியங்கள் தொடர்பான செய்திகள்", "24"),
        T("Periyapuranam, Nalayira Divya Prabandham, Thiruvilaiyadal Puranam, Thembavani, Seerapuranam", "பெரியபுராணம் – நாலாயிர திவ்வியப்பிரபந்தம் – திருவிளையாடற் புராணம் – தேம்பாவணி – சீறாப்புராணம் தொடர்பான செய்திகள்", "24"),
        T("Minor literature (Sitrilakkiyangal): Thirukkutralakuravanji, Kalingathuparani, Muthollayiram, Tamil Vidu Thoothu, Nandikkalambagam, Mukkoodarpallu, Kaavadichindhu, Muthukumaraswamy Pillaitamil, Rajarajan Ula", "சிற்றிலக்கியங்கள்: திருக்குற்றாலக்குறவஞ்சி – கலிங்கத்துப்பரணி – முத்தொள்ளாயிரம், தமிழ்விடு தூது – நந்திக்கலம்பகம் – முக்கூடற்பள்ளு – காவடிச்சிந்து - முத்துக்குமாரசாமி பிள்ளைத் தமிழ் - இராஜராஜ சோழன் உலா", "24"),
        T("Manonmaniyam, Panchali Sabadham, Kuyil Paattu, Irattura Mozhithal, Azhagiya Sokkanathar", "மனோன்மணியம் - பாஞ்சாலி சபதம் - குயில் பாட்டு - இரட்டுற மொழிதல் (காளமேகப் புலவர்) - அழகிய சொக்கநாதர் தொடர்பான செய்திகள்", "24"),
        T("Folk songs and Siddhar songs", "நாட்டுப்புறப் பாட்டு - சித்தர் பாடல்கள் தொடர்பான செய்திகள்", "24"),
        T("Religious pioneers: Appar, Sambandar, Sundarar, Manikkavasagar, Thirumoolar, Kulasekara Alvar, Andal, Seethalai Saathanar, H.A. Krishnapillai, Umaruppulavar", "சமய முன்னோடிகள் – அப்பர், சம்பந்தர், சுந்தரர், மாணிக்கவாசகர், திருமூலர், குலசேகர ஆழ்வார், ஆண்டாள், சீத்தலைச் சாத்தனார், எச்.ஏ.கிருட்டிணனார், உமறுப்புலவர் தொடர்பான செய்திகள், மேற்கோள்கள், சிறப்புப் பெயர்கள்", "24"),
    ]),
    ("Part C: Tamil scholars and service to Tamil", "பகுதி இ: தமிழ் அறிஞர்களும், தமிழ்த் தொண்டும்", [
        T("Bharathiyar, Bharathidasan, Namakkal Kavignar, Kavimani Desigavinayagam", "பாரதியார், பாரதிதாசன், நாமக்கல் கவிஞர், கவிமணி தேசிக விநாயகனார் தொடர்பான செய்திகள், சிறந்த தொடர்கள், சிறப்புப் பெயர்கள்", "24"),
        T("Traditional poetry: Mudiyarasan, Vanidasan, Surada, Kannadasan, Udumalai Narayana Kavi, Pattukkottai Kalyanasundaram, Marudhakasi", "மரபுக் கவிதை - முடியரசன், வாணிதாசன், சுரதா, கண்ணதாசன், உடுமலை நாராயண கவி, பட்டுக்கோட்டை கல்யாணசுந்தரம், மருதகாசி தொடர்பான செய்திகள், அடைமொழிப் பெயர்கள்", "24"),
        T("Modern (free-verse) poetry: Pichamoorthy, C.Su. Chellappa, Tharumu Sivaramu, Pasuvaiya, Ra. Meenakshi, Mu. Metha, Erodu Thamizhanban, Abdul Rahman, Kalapriya, Kalyanji, Gnanakoothan", "புதுக்கவிதை – ந.பிச்சமூர்த்தி, சி.சு.செல்லப்பா, தருமு சிவராமு, பசுவய்யா, இரா.மீனாட்சி, சி.மணி, சிற்பி, மு.மேத்தா, ஈரோடு தமிழன்பன், அப்துல்ரகுமான், கலாப்ரியா, கல்யாண்ஜி, ஞானக்கூத்தன் – தொடர்பான செய்திகள், மேற்கோள்கள், சிறப்புத் தொடர்கள் மற்றும் எழுதிய நூல்கள்", "24"),
        T("Letter literature in Tamil: Natkurippu, Nehru, Gandhi, Mu. Varadarasanar, Perarignar Anna", "தமிழில் கடித இலக்கியம் – நாட்குறிப்பு, ஜவகர்லால் நேரு, மகாத்மா காந்தி, மு.வரதராசனார், பேரறிஞர் அண்ணா தொடர்பான செய்திகள்", "24"),
        T("Performing (folk) arts", "நிகழ்கலை (நாட்டுப்புறக் கலைகள்) தொடர்பான செய்திகள்", "24"),
        T("Short stories in Tamil: titles, authors, matching", "தமிழில் சிறுகதைகள் தலைப்பு - ஆசிரியர் - பொருத்துதல்", "24"),
        T("Arts: sculpture, painting, oratory, cinema", "கலைகள் – சிற்பம் – ஓவியம் – பேச்சு - திரைப்படக்கலை தொடர்பான செய்திகள்", "24"),
        T("Antiquity of Tamil, greatness of the Tamil language, Dravidian languages", "தமிழின் தொன்மை - தமிழ்மொழியின் சிறப்பு, திராவிட மொழிகள் தொடர்பான செய்திகள்", "24"),
        T("Prose writers: Maraimalai Adigal, Parithimar Kalaignar, N.M. Venkatasami Nattar, Ra.Pi. Sethu, Thiru.Vi.Ka., Vaiyapuri, Devaneyap Pavanar, Seikkuthambi Pavalar", "உரைநடை - மறைமலை அடிகள், பரிதிமாற்கலைஞர், ந.மு.வேங்கடசாமி நாட்டார், ரா.பி.சேது, திரு.வி.கல்யாண சுந்தரனார், வையாபுரி, பேரா.தனிநாயகம் அடிகள், செய்குதம்பி பாவலர் – மொழி நடை தொடர்பான செய்திகள்", "24"),
        T("Tamil service of U.Ve. Swaminatha Iyer, Tha.Po. Meenakshisundaranar, C. Ilakkuvanar", "உ.வே.சாமிநாதர், தெ.பொ.மீனாட்சி சுந்தரனார், சி.இலக்குவனார் – தமிழ்ப்பணி தொடர்பான செய்திகள்", "24"),
        T("Devaneyap Pavanar (lexicon), Paavalareru Perunchithiranar; Tamil service", "தேவநேயப்பாவாணர்-அகரமுதலி, பாவலரேறு பெருஞ்சித்திரனார், தமிழ்த்தொண்டு தொடர்பான செய்திகள்", "24"),
        T("G.U. Pope and Veeramamunivar: Tamil service", "ஜி.யு.போப் - வீரமாமுனிவர் தமிழ்த்தொண்டு சிறப்புத் தொடர்கள்", "24"),
        T("Thanthai Periyar, Perarignar Anna, Muthuramalingar, Ambedkar, Kamarajar, Ma.Po.Sivagnanam, Kayidhe Millath: social service", "தந்தை பெரியார் – பேரறிஞர் அண்ணா – முத்துராமலிங்கர் – அம்பேத்கர் – காமராசர் – ம.பொ.சிவஞானம் - காயிதேமில்லத் - சமுதாயத் தொண்டு", "24"),
        T("Tamilagam: origins and changes of place names", "தமிழகம் – ஊரும் பேரும், தோற்றம் மாற்றம் பற்றிய செய்திகள்", "24"),
        T("Tamils across the world: their greatness and service to Tamil", "உலகளாவிய தமிழர்கள் சிறப்பும் – பெருமையும் – தமிழ்ப் பணியும்", "24"),
        T("Scientific thought in the Tamil language", "தமிழ்மொழியின் அறிவியல் சிந்தனைகள் தொடர்பான செய்திகள்", "24"),
        T("Women in Tamil: Moovalur Ramamirtham, Dr. Muthulakshmi, Velu Nachiyar, women achievers, women in the freedom struggle, Thillaiyadi Valliammai, Rani Mangammal, Annie Besant", "தமிழ் மகளிரின் சிறப்பு – மூவலூர் ராமாமிர்தம்மாள், டாக்டர் முத்துலட்சுமி அம்மையார், வேலு நாச்சியார் மற்றும் சாதனை மகளிர் - விடுதலைப் போராட்டத்தில் மகளிர் பங்கு - தில்லையாடி வள்ளியம்மை, ராணி மங்கம்மாள், அன்னி பெசன்ட் அம்மையார்", "24"),
        T("Tamil trade, archaeological studies and sea voyages", "தமிழர் வணிகம் – தொல்லியல் ஆய்வுகள் – கடற் பயணங்கள் – தொடர்பான செய்திகள்", "24"),
        T("Food as medicine; medicinal herbs", "உணவே மருந்து – நோய் தீர்க்கும் மூலிகைகள் தொடர்பான செய்திகள்", "24"),
        T("Religious harmony: Thayumanavar, Ramalinga Adigalar, Thiru.Vi.Ka.", "சமயப் பொதுமை உணர்த்திய தாயுமானவர், இராமலிங்க அடிகளார், திரு.வி.கல்யாண சுந்தரனார் தொடர்பான செய்திகள் – மேற்கோள்கள்", "24"),
        T("News about libraries", "நூலகம் பற்றிய செய்திகள்", "24"),
    ]),
])

# ---------------------------------------------------------------- General English (SSLC standard) -- G2 only (alternative to General Tamil)
def eng(name):
    return (name, "", "2")

subject("eng", "General English (SSLC standard)", "பொது ஆங்கிலம் (பத்தாம் வகுப்புத் தரம்)", [
    ("Part A: Grammar", "பகுதி அ: இலக்கணம்", [eng(n) for n in [
        "Match words and phrases with their meanings", "Synonyms", "Antonyms", "Prefixes and suffixes", "Articles",
        "Prepositions", "Question tags", "Tenses", "Voice", "Infinitives, gerunds and participles",
        "Sentence patterns (subject, verb, object...)", "Homophones", "Find the error", "Select the correct sentence",
        "Odd word out (verb, noun, adjective, adverb)", "Plural forms", "Simple, compound and complex sentences",
        "Degrees of comparison", "Blending words", "Compound words", "British English and American English"]]),
    ("Part B: Poetry", "பகுதி ஆ: கவிதை", [eng(n) for n in [
        "Figures of speech (alliteration, simile, metaphor, personification, onomatopoeia, anaphora, rhyme scheme, rhyming words, repetition)",
        "Poetry appreciation", "Important lines",
        "Life - Henry Van Dyke", "I am Every Woman - Rakhi Nariani Shirke", "The Secret of the Machines - Rudyard Kipling",
        "The Ant and The Cricket - adapted from Aesop's fables", "No Men are Foreign - James Falconer Kirkup",
        "The House on Elm Street - Nadia Bush", "Stopping by Woods on a Snowy Evening - Robert Frost",
        "A Poison Tree - William Blake", "On Killing a Tree - Gieve Patel", "The Spider and the Fly - Mary Botham Howitt",
        "The River - Caroline Ann Bowles", "The Comet - Norman Littleford", "The Stick-together Families - Edgar Albert Guest",
        "Special Hero - Christina M. Kerschen", "Making Life Worth While - George Eliot", "A Thing of Beauty - John Keats",
        "Lessons in Life - Brigette Bryant and Daniel Ho", "My Computer Needs a Break - Shanthini Govindan",
        "Your Space - David Bates", "Sea Fever - John Masefield", "Courage - Edgar Albert Guest",
        "Team Work - Edgar Albert Guest", "From a Railway Carriage - Robert Louis Stevenson",
        "Indian Seasons - Nisha Dyrene", "A Tragic Story - William Makepeace Thackeray"]]),
    ("Part C: Literary works", "பகுதி இ: இலக்கியப் படைப்புகள்", [eng(n) for n in [
        "His First Flight - Liam O'Flaherty", "The Tempest - Tales from Shakespeare", "The Last Lesson - Alphonse Daudet",
        "The Little Hero of Holland - Mary Mapes Dodge", "The Dying Detective - Arthur Conan Doyle",
        "Learning the Game - Sachin Tendulkar", "The Cat and the Painkiller - Mark Twain",
        "Water, the Elixir of Life - Sir C.V. Raman", "The Story of a Grizzly Cub - William Temple Hornaday",
        "Sir Isaac Newton - Nathaniel Hawthorne", "My Reminiscence - Rabindranath Tagore",
        "The Woman on Platform 8 - Ruskin Bond", "The Nose Jewel - C. Rajagopalachari", "A Birthday Letter - Jawaharlal Nehru",
        "Biographies: Mahatma Gandhi, Jawaharlal Nehru, Subash Chandra Bose, Helen Keller, Kalpana Chawla, Dr. Salim Ali, Rani of Jhansi, Nelson Mandela, Abraham Lincoln",
        "General comprehension"]]),
])


def build():
    subjects = []
    for sid, sen, sta, units in SUBJECTS:
        us = []
        for ui, (uen, uta, topics) in enumerate(units, 1):
            ts = []
            for ti, (ten, tta, ex) in enumerate(topics, 1):
                ts.append({
                    "id": f"{sid}.u{ui}.t{ti:02d}",
                    "nameEn": ten,
                    "nameTa": tta,
                    "exams": [CODE[c] for c in ex],
                })
            us.append({"id": f"{sid}.u{ui}", "nameEn": uen, "nameTa": uta, "topics": ts})
        subjects.append({"id": sid, "nameEn": sen, "nameTa": sta, "units": us})
    return {
        "version": 2,
        "_notice": ("Built from the official TNPSC syllabus documents: Group I prelims (code 497, dated 16.12.2024), "
                    "Group II/IIA scheme and syllabus (revised 27.01.2022) and Group IV/VAO scheme and syllabus "
                    "(revised 27.01.2022). Topics are split from the official bullet points and the exam tags come from "
                    "comparing the three documents; this is a study aid, so always check the current official notification. "
                    "Tamil names follow the official Tamil text where it was readable; a few (marked in the project notes) "
                    "are drafts that need a native speaker's review. IDs are stable: never renumber, only add."),
        "subjects": subjects,
    }


if __name__ == "__main__":
    out = sys.argv[1]
    data = build()
    with open(out, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    topics = [t for s in data["subjects"] for u in s["units"] for t in u["topics"]]
    print("subjects", len(data["subjects"]), "topics", len(topics))
    for code in ("G1", "G2", "G4"):
        print(code, sum(code in t["exams"] for t in topics))
