#!/usr/bin/env python3
"""Generates app/src/main/assets/current_affairs.json (pack v1).

Every item is written in my own words. For items derived from Wikipedia's "2026 in India", `match` is a substring of the
article's bullet; the script looks up the news URL cited by that bullet (and asserts it matches exactly one bullet and the
right month) so links are never typed from memory. Items from other sources give `url` directly.
"""
import json, re, sys

WIKI_IN = "https://en.wikipedia.org/wiki/2026_in_India"
LINES = []  # (month, text, url)
for raw in open("india_clean.txt", encoding="utf-8"):
    m = re.match(r"^\[([A-Za-z ]+)\]\s+(.*?)(?:\s+<(https?://[^>]+)>)?$", raw.rstrip("\n"))
    if m:
        LINES.append((m.group(1), m.group(2), m.group(3)))

MONTHS = {"01": "January", "02": "February", "03": "March", "04": "April", "05": "May", "06": "June",
          "07": "July", "08": "August", "09": "September", "10": "October", "11": "November"}

ITEMS = []
ERRORS = []


def wiki(id_, date, cat, en_t, en_s, ta_t, ta_s, match):
    hits = [l for l in LINES if match in l[1]]
    if len(hits) != 1:
        ERRORS.append(f"{id_}: match {match!r} found {len(hits)} bullets: " + " | ".join(h[0] + ":" + h[1][:50] for h in hits))
        return
    month, _, url = hits[0]
    if month != MONTHS[date[5:7]]:
        ERRORS.append(f"{id_}: bullet is under {month} but date says {date}")
    host = re.sub(r"^https?://(www\.)?", "", url or WIKI_IN).split("/")[0]
    ITEMS.append(dict(id=id_, date=date, category=cat, titleEn=en_t, titleTa=ta_t, summaryEn=en_s, summaryTa=ta_s,
                      sourceName=f"{host} (via Wikipedia '2026 in India')", url=url or WIKI_IN, verified=False))


def other(id_, date, cat, en_t, en_s, ta_t, ta_s, source, url):
    ITEMS.append(dict(id=id_, date=date, category=cat, titleEn=en_t, titleTa=ta_t, summaryEn=en_s, summaryTa=ta_s,
                      sourceName=source, url=url, verified=False))


# ------------------------------------------------------------------ January
wiki("ca-2026-01-19-bjp-president", "2026-01-19", "POLITY",
     "Nitin Nabin elected BJP national president",
     "Nitin Nabin was elected National President of the Bharatiya Janata Party.",
     "பாஜக தேசியத் தலைவராக நிதின் நபின் தேர்வு",
     "நிதின் நபின் பாரதிய ஜனதா கட்சியின் தேசியத் தலைவராகத் தேர்ந்தெடுக்கப்பட்டார்.", "Nitin Nabin")
other("ca-2026-01-25-padma-awards", "2026-01-25", "AWARDS",
      "Padma Awards 2026 announced: 131 awardees",
      "On the eve of Republic Day, 131 Padma Awards were announced: 5 Padma Vibhushan, 13 Padma Bhushan and 113 Padma Shri. Actor Dharmendra and former Kerala Chief Minister V. S. Achuthanandan (both posthumously) are among the Padma Vibhushan recipients. Check the Padma Awards portal for the full list.",
      "2026 பத்ம விருதுகள் அறிவிப்பு: 131 பேர்",
      "குடியரசு தினத்தை முன்னிட்டு 131 பத்ம விருதுகள் அறிவிக்கப்பட்டன: 5 பத்ம விபூஷண், 13 பத்ம பூஷண், 113 பத்மஸ்ரீ. நடிகர் தர்மேந்திரா, கேரள முன்னாள் முதல்வர் வி.எஸ். அச்சுதானந்தன் (இருவரும் மறைவுக்குப் பின்) பத்ம விபூஷண் பெறுவோரில் உள்ளனர். முழு பட்டியலுக்கு பத்ம விருதுகள் இணையதளத்தைப் பார்க்கவும்.",
      "thenewsminute.com", "https://www.thenewsminute.com/amp/story/news/padma-awards-2026-131-recipients-named-mammootty-alka-yagnik-among-awardees")
wiki("ca-2026-01-27-india-eu-fta", "2026-01-27", "ECONOMY",
     "India and the European Union reach a free trade agreement",
     "India and the European Union announced that they had reached a free trade agreement.",
     "இந்தியா - ஐரோப்பிய ஒன்றியம் தடையற்ற வர்த்தக ஒப்பந்தம்",
     "இந்தியாவும் ஐரோப்பிய ஒன்றியமும் தடையற்ற வர்த்தக ஒப்பந்தத்தை எட்டியதாக அறிவித்தன.", "India and the European Union reach")
wiki("ca-2026-01-28-baramati-crash", "2026-01-28", "INDIA",
     "Maharashtra Deputy CM Ajit Pawar killed in Baramati plane crash",
     "A Learjet 45 crashed and exploded at Baramati Airport in Maharashtra, killing five people including Deputy Chief Minister Ajit Pawar.",
     "பாராமதி விமான விபத்தில் மகாராஷ்டிர துணை முதல்வர் அஜித் பவார் உயிரிழப்பு",
     "மகாராஷ்டிராவின் பாராமதி விமான நிலையத்தில் லியர்ஜெட் 45 விமானம் விழுந்து வெடித்ததில் துணை முதல்வர் அஜித் பவார் உட்பட ஐந்து பேர் உயிரிழந்தனர்.", "Baramati Learjet 45")
wiki("ca-2026-01-29-ugc-equity-stay", "2026-01-29", "POLITY",
     "Supreme Court stays the UGC Equity Regulations, 2026",
     "The Supreme Court stayed the implementation of the UGC Equity Regulations, 2026, after protests across the country over the UGC Bill 2026 (the Court cited possible misuse).",
     "யுஜிசி சமத்துவ விதிகள் 2026-க்கு உச்ச நீதிமன்றம் தடை",
     "யுஜிசி மசோதா 2026-க்கு எதிராக நாடு முழுவதும் போராட்டங்கள் நடந்த நிலையில், யுஜிசி சமத்துவ விதிகள் 2026-ஐ அமல்படுத்த உச்ச நீதிமன்றம் தடை விதித்தது (தவறான பயன்பாட்டுக்கு வாய்ப்பு உள்ளதாகக் குறிப்பிட்டது).", "UGC Equity Regulations")
wiki("ca-2026-01-21-nipah-wb", "2026-01-21", "INDIA",
     "Nipah virus outbreak reported in West Bengal",
     "An outbreak of Nipah virus was reported in West Bengal, with at least two cases recorded in Barasat.",
     "மேற்கு வங்கத்தில் நிபா வைரஸ் பாதிப்பு",
     "மேற்கு வங்கத்தின் பராசத் பகுதியில் குறைந்தது இரண்டு நிபா வைரஸ் பாதிப்புகள் பதிவாகின.", "Nipah virus")

# ------------------------------------------------------------------ February
other("ca-2026-02-01-union-budget", "2026-02-01", "ECONOMY",
      "Union Budget 2026-27 presented (Sitharaman's ninth)",
      "Finance Minister Nirmala Sitharaman presented her ninth Union Budget on 1 February 2026. Reported highlights: fiscal deficit target 4.3% of GDP for 2026-27; no change in income tax rates; the new Income Tax Act, 2025 takes effect from 1 April 2026; revised-return deadline extended from 31 December to 31 March; total expenditure about Rs 53.5 lakh crore. Confirm figures in the official budget documents (indiabudget.gov.in).",
      "மத்திய பட்ஜெட் 2026-27 தாக்கல் (சீதாராமனின் ஒன்பதாவது)",
      "நிதியமைச்சர் நிர்மலா சீதாராமன் 2026 பிப்ரவரி 1 அன்று தனது ஒன்பதாவது மத்திய பட்ஜெட்டை தாக்கல் செய்தார். செய்திகளின்படி: 2026-27 நிதிப் பற்றாக்குறை இலக்கு GDP-யில் 4.3%; வருமான வரி விகிதங்களில் மாற்றம் இல்லை; புதிய வருமான வரிச் சட்டம் 2025 ஏப்ரல் 1 முதல் அமல்; திருத்திய கணக்கு தாக்கல் காலக்கெடு டிசம்பர் 31-இலிருந்து மார்ச் 31-க்கு நீட்டிப்பு; மொத்தச் செலவு சுமார் ரூ.53.5 லட்சம் கோடி. எண்களை அதிகாரப்பூர்வ பட்ஜெட் ஆவணங்களில் சரிபார்க்கவும்.",
      "scconline.com", "https://www.scconline.com/blog/post/2026/02/02/union-budget-2026-27-highlights-key-announcements/")
wiki("ca-2026-02-02-us-india-trade", "2026-02-02", "ECONOMY",
     "US announces trade deal with India; tariffs cut to 18%",
     "US President Donald Trump announced a trade agreement that reduces US tariffs on Indian exports to 18%, in exchange for India stopping purchases of Russian oil (as reported).",
     "இந்தியாவுடன் அமெரிக்க வர்த்தக ஒப்பந்தம்: வரி 18% ஆகக் குறைப்பு",
     "இந்திய ஏற்றுமதிகள் மீதான அமெரிக்க வரியை 18% ஆகக் குறைக்கும் வர்த்தக ஒப்பந்தத்தை அதிபர் டிரம்ப் அறிவித்தார்; இதற்கு ஈடாக இந்தியா ரஷ்ய எண்ணெய் வாங்குவதை நிறுத்தும் என்று செய்திகள் தெரிவித்தன.", "reduction of US tariffs on Indian exports")
wiki("ca-2026-02-04-manipur-cm", "2026-02-04", "POLITY",
     "Yumnam Khemchand Singh sworn in as Manipur Chief Minister",
     "Yumnam Khemchand Singh took oath as Chief Minister of Manipur after President's rule in the state ended.",
     "மணிப்பூர் முதல்வராக யும்னம் கெம்சந்த் சிங் பதவியேற்பு",
     "குடியரசுத் தலைவர் ஆட்சி முடிவுக்கு வந்த பின் யும்னம் கெம்சந்த் சிங் மணிப்பூர் முதல்வராகப் பதவியேற்றார்.", "Yumnam Khemchand Singh")
other("ca-2026-02-22-winter-olympics", "2026-02-22", "SPORTS",
      "Winter Olympics 2026 held in Milano Cortina, Italy",
      "The XXV Olympic Winter Games were held from 6 to 22 February 2026 across Lombardy, Veneto and Trentino-Alto Adige. Ski mountaineering made its Olympic debut, and these were the first Games under IOC President Kirsty Coventry.",
      "2026 குளிர்கால ஒலிம்பிக் - மிலானோ கார்டினா (இத்தாலி)",
      "XXV குளிர்கால ஒலிம்பிக் போட்டிகள் 2026 பிப்ரவரி 6 முதல் 22 வரை லோம்பார்டி, வெனெட்டோ, ட்ரென்டினோ-ஆல்டோ அடிஜே பகுதிகளில் நடைபெற்றன. ஸ்கீ மவுண்டனியரிங் முதல்முறையாக ஒலிம்பிக்கில் இடம்பெற்றது; ஐஓசி தலைவர் கிர்ஸ்டி கோவென்ட்ரியின் கீழ் நடந்த முதல் போட்டி இது.",
      "Wikipedia '2026 Winter Olympics'", "https://en.wikipedia.org/wiki/2026_Winter_Olympics")
wiki("ca-2026-02-16-ai-summit", "2026-02-16", "SCIENCE",
     "AI Impact Summit held in New Delhi (16-20 February)",
     "New Delhi hosted the AI Impact Summit from 16 to 20 February 2026.",
     "புது தில்லியில் AI தாக்க உச்சி மாநாடு (பிப்ரவரி 16-20)",
     "2026 பிப்ரவரி 16 முதல் 20 வரை புது தில்லியில் AI தாக்க உச்சி மாநாடு நடைபெற்றது.", "AI Impact Summit")
wiki("ca-2026-02-21-brazil-minerals", "2026-02-21", "WORLD",
     "India and Brazil sign critical minerals agreement",
     "Brazil and India signed an agreement to strengthen cooperation on critical minerals and rare earth elements.",
     "இந்தியா - பிரேசில் முக்கிய கனிமங்கள் ஒப்பந்தம்",
     "முக்கிய கனிமங்கள் மற்றும் அரிய தனிமங்கள் தொடர்பான ஒத்துழைப்பை வலுப்படுத்த பிரேசிலும் இந்தியாவும் ஒப்பந்தம் செய்தன.", "critical minerals and rare earth")
wiki("ca-2026-02-27-liquor-case", "2026-02-27", "POLITY",
     "Delhi court discharges Kejriwal, Sisodia and others in liquor scam case",
     "A special court in Delhi discharged Arvind Kejriwal, Manish Sisodia, K. Kavitha and 22 others in the Delhi liquor scam case.",
     "தில்லி மதுபான முறைகேடு வழக்கில் கேஜ்ரிவால், சிசோடியா உள்ளிட்டோர் விடுவிப்பு",
     "தில்லி சிறப்பு நீதிமன்றம் மதுபான முறைகேடு வழக்கில் அரவிந்த் கேஜ்ரிவால், மணீஷ் சிசோடியா, கே. கவிதா உள்ளிட்ட 25 பேரை விடுவித்தது.", "Delhi liquor scam")

# ------------------------------------------------------------------ March
wiki("ca-2026-03-02-india-canada", "2026-03-02", "WORLD",
     "India and Canada agree an economic partnership deal",
     "Prime Ministers Narendra Modi and Mark Carney announced a deal to strengthen the India-Canada economic partnership, after two years of strained relations.",
     "இந்தியா - கனடா பொருளாதார கூட்டாண்மை ஒப்பந்தம்",
     "இரண்டு ஆண்டுகால பதற்றத்துக்குப் பின், பிரதமர் மோடியும் கனடா பிரதமர் மார்க் கார்னியும் இருநாட்டுப் பொருளாதாரக் கூட்டாண்மையை வலுப்படுத்தும் ஒப்பந்தத்தை அறிவித்தனர்.", "Mark Carney")
wiki("ca-2026-03-04-russian-oil-waiver", "2026-03-04", "ECONOMY",
     "US allows India to buy Russian oil until 4 April",
     "The United States allowed India to buy Russian oil until 4 April 2026.",
     "ரஷ்ய எண்ணெய் வாங்க ஏப்ரல் 4 வரை அமெரிக்கா அனுமதி",
     "இந்தியா 2026 ஏப்ரல் 4 வரை ரஷ்ய எண்ணெய் வாங்க அமெரிக்கா அனுமதித்தது.", "buy Russian oil until 4 April")
wiki("ca-2026-03-08-t20-world-cup", "2026-03-08", "SPORTS",
     "India win the 2026 Men's T20 World Cup",
     "India beat New Zealand in the final on 8 March 2026 to win the ICC Men's T20 World Cup, which was co-hosted by India and Sri Lanka (7 February to 8 March).",
     "2026 ஆடவர் T20 உலகக் கோப்பையை இந்தியா வென்றது",
     "2026 மார்ச் 8 அன்று இறுதிப் போட்டியில் நியூசிலாந்தை வென்று இந்தியா ICC ஆடவர் T20 உலகக் கோப்பையை வென்றது. போட்டிகள் இந்தியா, இலங்கை இணைந்து (பிப்ரவரி 7 - மார்ச் 8) நடத்தின.", "win the 2026 Men's T20 World Cup")
wiki("ca-2026-03-11-speaker-no-confidence", "2026-03-11", "POLITY",
     "Lok Sabha rejects no-confidence motion against Speaker Om Birla",
     "The Lok Sabha rejected, by voice vote, a no-confidence motion against Speaker Om Birla.",
     "சபாநாயகர் ஓம் பிர்லாவுக்கு எதிரான நம்பிக்கையில்லா தீர்மானம் தோல்வி",
     "சபாநாயகர் ஓம் பிர்லாவுக்கு எதிரான நம்பிக்கையில்லா தீர்மானத்தை மக்களவை குரல் வாக்கெடுப்பில் நிராகரித்தது.", "no confidence vote against speaker")
wiki("ca-2026-03-11-harish-rana", "2026-03-11", "POLITY",
     "Supreme Court allows withdrawal of life support in Harish Rana case",
     "The Supreme Court ruled in favour of removing life support for Harish Rana, who had been in a vegetative state since a fall in 2013.",
     "ஹரிஷ் ராணா வழக்கில் உயிர்காப்பு சிகிச்சையை நிறுத்த உச்ச நீதிமன்றம் அனுமதி",
     "2013-இல் ஏற்பட்ட வீழ்ச்சிக்குப் பின் நினைவற்ற நிலையில் இருந்த ஹரிஷ் ராணாவுக்கு உயிர்காப்பு சிகிச்சையை நிறுத்த உச்ச நீதிமன்றம் தீர்ப்பளித்தது.", "Harish Rana")
wiki("ca-2026-03-14-wangchuk", "2026-03-14", "INDIA",
     "Sonam Wangchuk released after NSA order quashed",
     "Sonam Wangchuk was released from jail after the government quashed the National Security Act order against him, following the Ladakh protests.",
     "தேசியப் பாதுகாப்புச் சட்ட உத்தரவு ரத்து: சோனம் வாங்சுக் விடுதலை",
     "லடாக் போராட்டங்களுக்குப் பின் சோனம் வாங்சுக்கிற்கு எதிரான தேசியப் பாதுகாப்புச் சட்ட உத்தரவை அரசு ரத்து செய்ததால் அவர் சிறையிலிருந்து விடுவிக்கப்பட்டார்.", "Sonam Wangchuk is released")
wiki("ca-2026-03-19-dental-commission", "2026-03-19", "INDIA",
     "National Dental Commission constituted",
     "The Government of India constituted the National Dental Commission, replacing the Dental Council of India set up under the Dentists Act, 1948.",
     "தேசிய பல் மருத்துவ ஆணையம் அமைப்பு",
     "பல் மருத்துவர்கள் சட்டம், 1948-இன் கீழ் இருந்த இந்திய பல் மருத்துவ கவுன்சிலுக்குப் பதிலாக தேசிய பல் மருத்துவ ஆணையத்தை மத்திய அரசு அமைத்தது.", "National Dental Commission")
wiki("ca-2026-03-24-andrabi", "2026-03-24", "INDIA",
     "Asiya Andrabi sentenced to life imprisonment",
     "A special NIA court sentenced Kashmiri separatist Asiya Andrabi to life imprisonment for her involvement in terrorist activities.",
     "ஆசியா அந்த்ராபிக்கு ஆயுள் தண்டனை",
     "பயங்கரவாதச் செயல்களில் ஈடுபட்டதற்காக காஷ்மீர் பிரிவினைவாதி ஆசியா அந்த்ராபிக்கு சிறப்பு NIA நீதிமன்றம் ஆயுள் தண்டனை விதித்தது.", "Asiya Andrabi")
wiki("ca-2026-03-28-noida-airport", "2026-03-28", "INDIA",
     "Noida International Airport inaugurated",
     "Prime Minister Narendra Modi inaugurated the Noida International Airport.",
     "நொய்டா சர்வதேச விமான நிலையம் திறப்பு",
     "பிரதமர் நரேந்திர மோடி நொய்டா சர்வதேச விமான நிலையத்தைத் திறந்து வைத்தார்.", "Noida International Airport")
wiki("ca-2026-03-30-naxal", "2026-03-30", "INDIA",
     "Government says the Naxalite insurgency has ended",
     "Home Minister Amit Shah reported that most remaining Naxal fighters had been killed, arrested or had surrendered, and the government said the insurgency had ended. Some senior commanders said they would continue the armed struggle.",
     "நக்சல் கிளர்ச்சி முடிவுக்கு வந்ததாக அரசு அறிவிப்பு",
     "எஞ்சிய நக்சல்களில் பெரும்பாலோர் கொல்லப்பட்டனர், கைது செய்யப்பட்டனர் அல்லது சரணடைந்தனர் என உள்துறை அமைச்சர் அமித் ஷா தெரிவித்து, கிளர்ச்சி முடிந்ததாக அரசு கூறியது. சில மூத்த தளபதிகள் ஆயுதப் போராட்டத்தைத் தொடர்வோம் என்றனர்.", "ended the Naxalite insurgency")

# ------------------------------------------------------------------ April
wiki("ca-2026-04-06-sathankulam", "2026-04-06", "TN",
     "Sathankulam custodial deaths case: nine policemen sentenced to death",
     "A special court in Madurai sentenced all nine police personnel accused in the 2020 Sathankulam custodial death case to death; the court had convicted them on 23 March 2026.",
     "சாத்தான்குளம் காவல் மரண வழக்கு: ஒன்பது காவலர்களுக்கு மரண தண்டனை",
     "2020 சாத்தான்குளம் காவல் மரண வழக்கில் குற்றம்சாட்டப்பட்ட ஒன்பது காவலர்களுக்கும் மதுரை சிறப்பு நீதிமன்றம் மரண தண்டனை விதித்தது; அவர்கள் 2026 மார்ச் 23 அன்று குற்றவாளிகளாக அறிவிக்கப்பட்டிருந்தனர்.", "sentences all nine policemen")
wiki("ca-2026-04-06-telangana-cancer", "2026-04-06", "INDIA",
     "Telangana makes cancer a notifiable disease",
     "Telangana made cancer a notifiable disease and mandated reporting within one month.",
     "புற்றுநோயை அறிவிக்கத்தக்க நோயாக தெலங்கானா அறிவிப்பு",
     "புற்றுநோயை அறிவிக்கத்தக்க நோயாக தெலங்கானா அறிவித்து, ஒரு மாதத்துக்குள் தெரிவிக்க வேண்டும் என கட்டாயமாக்கியது.", "notifiable disease")
wiki("ca-2026-04-11-hormuz", "2026-04-11", "WORLD",
     "First Indian vessel crosses Strait of Hormuz after Iran war ceasefire",
     "The India-flagged LPG tanker Jag Vikram crossed the Strait of Hormuz, the first Indian vessel to do so since the ceasefire in the 2026 Iran war.",
     "ஈரான் போர் நிறுத்தத்துக்குப் பின் ஹார்முஸ் நீரிணையைக் கடந்த முதல் இந்தியக் கப்பல்",
     "2026 ஈரான் போரில் போர்நிறுத்தத்துக்குப் பின், இந்தியக் கொடியுடன் கூடிய ஜக் விக்ரம் எல்பிஜி கப்பல் ஹார்முஸ் நீரிணையைக் கடந்த முதல் இந்தியக் கப்பலானது.", "Jag Vikram")
wiki("ca-2026-04-14-bihar-cm", "2026-04-14", "POLITY",
     "Samrat Choudhary succeeds Nitish Kumar as Bihar Chief Minister",
     "Nitish Kumar resigned as Chief Minister of Bihar (he had announced on 5 March that he would contest the Rajya Sabha elections) and Samrat Choudhary succeeded him.",
     "பிஹார் முதல்வராக சாம்ராட் சௌத்ரி",
     "நிதீஷ் குமார் பிஹார் முதல்வர் பதவியிலிருந்து விலகினார் (மார்ச் 5 அன்று மாநிலங்களவைத் தேர்தலில் போட்டியிடப் போவதாக அறிவித்திருந்தார்); சாம்ராட் சௌத்ரி அவருக்குப் பின் முதல்வரானார்.", "Samrat Choudhary")
wiki("ca-2026-04-17-delimitation", "2026-04-17", "POLITY",
     "Delimitation Bill and 131st Amendment Bill fail in Lok Sabha",
     "The Delimitation Bill, 2026 and the Constitution (131st Amendment) Bill, 2026, which proposed raising Lok Sabha seats from 543 to 850 to implement the Women's Reservation Bill, failed to pass because they lacked a two-thirds majority in the Lok Sabha.",
     "தொகுதி மறுசீரமைப்பு மசோதா, 131-வது திருத்த மசோதா மக்களவையில் தோல்வி",
     "மகளிர் இட ஒதுக்கீட்டைச் செயல்படுத்த மக்களவை இடங்களை 543-இலிருந்து 850 ஆக உயர்த்த முன்மொழிந்த தொகுதி மறுசீரமைப்பு மசோதா 2026 மற்றும் அரசியலமைப்பு (131-வது திருத்த) மசோதா 2026 ஆகியவை மக்களவையில் மூன்றில் இரண்டு பங்கு பெரும்பான்மை இல்லாததால் நிறைவேறவில்லை.", "Delimitation Bill")
wiki("ca-2026-04-19-virudhunagar", "2026-04-19", "TN",
     "Firecracker factory explosion in Virudhunagar kills 23",
     "At least 23 people were killed and six injured in an explosion at a firecracker factory in Virudhunagar, Tamil Nadu.",
     "விருதுநகர் பட்டாசு ஆலை வெடி விபத்தில் 23 பேர் உயிரிழப்பு",
     "தமிழ்நாட்டின் விருதுநகரில் பட்டாசு ஆலையில் ஏற்பட்ட வெடி விபத்தில் குறைந்தது 23 பேர் உயிரிழந்தனர்; ஆறு பேர் காயமடைந்தனர்.", "firecrackers factory in Virudhunagar")
wiki("ca-2026-04-28-nz-fta-signed", "2026-04-28", "ECONOMY",
     "New Zealand-India Free Trade Agreement signed",
     "The New Zealand-India Free Trade Agreement was signed in New Delhi. It came into effect on 20 October 2026.",
     "நியூசிலாந்து - இந்தியா தடையற்ற வர்த்தக ஒப்பந்தம் கையெழுத்து",
     "நியூசிலாந்து - இந்தியா தடையற்ற வர்த்தக ஒப்பந்தம் புது தில்லியில் கையெழுத்தானது. இது 2026 அக்டோபர் 20 முதல் அமலுக்கு வந்தது.", "New Zealand–India Free Trade Agreement is signed")

# ------------------------------------------------------------------ May
other("ca-2026-05-04-assembly-results", "2026-05-04", "POLITY",
      "Results of the 2026 Assembly elections (TN, Kerala, West Bengal, Assam, Puducherry)",
      "Counting for the April 2026 Assembly elections took place on 4 May. Tamil Nadu: TVK won in its first election, ending a 59-year dominance of Dravidian parties (turnout 85.1%, the highest in the state's history). Kerala: UDF won a majority (V. D. Satheesan became CM). West Bengal: BJP won a landslide and formed its first government in the state, defeating the incumbent Trinamool Congress. Assam: NDA won a landslide. Puducherry: all 30 seats were counted.",
      "2026 சட்டமன்றத் தேர்தல் முடிவுகள் (தமிழ்நாடு, கேரளம், மேற்கு வங்கம், அசாம், புதுச்சேரி)",
      "2026 ஏப்ரலில் நடந்த சட்டமன்றத் தேர்தல்களின் வாக்கு எண்ணிக்கை மே 4 அன்று நடைபெற்றது. தமிழ்நாடு: முதல் தேர்தலிலேயே தவெக வென்று 59 ஆண்டுகால திராவிடக் கட்சிகளின் ஆதிக்கத்தை முடிவுக்குக் கொண்டுவந்தது (வாக்குப்பதிவு 85.1% - மாநில வரலாற்றில் அதிகம்). கேரளம்: ஐக்கிய ஜனநாயக முன்னணி (UDF) பெரும்பான்மை பெற்றது (வி.டி. சதீசன் முதல்வரானார்). மேற்கு வங்கம்: பாஜக அமோக வெற்றி பெற்று அம்மாநிலத்தில் முதல்முறையாக ஆட்சி அமைத்தது; ஆளும் திரிணமூல் காங்கிரஸ் தோற்றது. அசாம்: தேஜகூ அமோக வெற்றி. புதுச்சேரி: 30 தொகுதிகளுக்கும் வாக்குகள் எண்ணப்பட்டன.",
      "Wikipedia election articles", "https://en.wikipedia.org/wiki/2026_Tamil_Nadu_Legislative_Assembly_election")
wiki("ca-2026-05-09-wb-cm", "2026-05-09", "POLITY",
     "Suvendu Adhikari sworn in as West Bengal Chief Minister",
     "Suvendu Adhikari was inaugurated as Chief Minister of West Bengal.",
     "மேற்கு வங்க முதல்வராக சுவேந்து அதிகாரி பதவியேற்பு",
     "சுவேந்து அதிகாரி மேற்கு வங்க முதல்வராகப் பதவியேற்றார்.", "Suvendu Adhikari")
wiki("ca-2026-05-10-vijay-cm", "2026-05-10", "TN",
     "C. Joseph Vijay (TVK) takes oath as Tamil Nadu Chief Minister",
     "Vijay, founder of Tamilaga Vettri Kazhagam (TVK, founded 2 February 2024), took oath as the 17th Chief Minister of Tamil Nadu. His first orders reportedly covered 200 units of free electricity for households, a women's safety force (Singappen Athiradi Padai) and anti-drug units in every district.",
     "தவெக தலைவர் விஜய் தமிழ்நாடு முதல்வராகப் பதவியேற்பு",
     "தமிழக வெற்றிக் கழகத்தின் (தவெக - 2024 பிப்ரவரி 2 அன்று தொடங்கப்பட்டது) நிறுவனர் சி. ஜோசப் விஜய் தமிழ்நாட்டின் 17-வது முதல்வராகப் பதவியேற்றார். வீடுகளுக்கு 200 யூனிட் இலவச மின்சாரம், பெண்கள் பாதுகாப்புப் படை (சிங்கப்பெண் அதிரடிப் படை), ஒவ்வொரு மாவட்டத்திலும் போதைப்பொருள் தடுப்புப் பிரிவுகள் ஆகியவை அவரது முதல் உத்தரவுகளில் இடம்பெற்றதாகச் செய்திகள் கூறுகின்றன.", "Vijay takes oath as the Chief Minister of Tamil Nadu")
wiki("ca-2026-05-12-neet-cancelled", "2026-05-12", "INDIA",
     "NEET-UG 2026 cancelled after paper leak",
     "The National Testing Agency cancelled the NEET (UG) 2026, held on 3 May for over 2.27 million candidates, after a paper leak: investigators found substantial overlap with a 'guess paper' circulated before the exam, and several people including NTA officials were arrested.",
     "வினாத்தாள் கசிவால் NEET-UG 2026 ரத்து",
     "மே 3 அன்று 22.7 லட்சத்துக்கும் மேற்பட்டோர் எழுதிய NEET (UG) 2026 தேர்வை வினாத்தாள் கசிவால் தேசிய தேர்வு முகமை (NTA) ரத்து செய்தது. தேர்வுக்கு முன் பரவிய 'கெஸ் பேப்பரில்' பெருமளவு ஒற்றுமை இருந்ததைக் கண்டறிந்த புலனாய்வாளர்கள் NTA அதிகாரிகள் உட்பட பலரைக் கைது செய்தனர்.", "cancellation of the 2026 National Eligibility")
wiki("ca-2026-05-14-kerala-cm", "2026-05-14", "POLITY",
     "V. D. Satheesan elected Chief Minister of Kerala",
     "V. D. Satheesan was elected Chief Minister of Kerala after the UDF won the Assembly election.",
     "கேரள முதல்வராக வி.டி. சதீசன் தேர்வு",
     "ஐக்கிய ஜனநாயக முன்னணி தேர்தலில் வென்றதையடுத்து வி.டி. சதீசன் கேரள முதல்வராகத் தேர்ந்தெடுக்கப்பட்டார்.", "V. D. Satheesan is elected")
wiki("ca-2026-05-27-sir-upheld", "2026-05-27", "POLITY",
     "Supreme Court upholds validity of the Special Intensive Revision",
     "In a landmark verdict, the Supreme Court upheld the constitutional validity of the Special Intensive Revision (of electoral rolls).",
     "சிறப்புத் தீவிர திருத்தத்தின் செல்லுபடித் தன்மையை உச்ச நீதிமன்றம் உறுதி செய்தது",
     "ஒரு முக்கியத் தீர்ப்பில், (வாக்காளர் பட்டியலின்) சிறப்புத் தீவிர திருத்தத்தின் அரசியலமைப்புச் செல்லுபடித் தன்மையை உச்ச நீதிமன்றம் உறுதி செய்தது.", "Special Intensive Revision")
wiki("ca-2026-05-28-karnataka-cm", "2026-05-28", "POLITY",
     "D. K. Shivakumar becomes Karnataka Chief Minister",
     "D. K. Shivakumar was selected as the new Chief Minister of Karnataka, replacing Siddaramaiah.",
     "கர்நாடக முதல்வராக டி.கே. சிவகுமார்",
     "சித்தராமையாவுக்குப் பதிலாக டி.கே. சிவகுமார் கர்நாடகத்தின் புதிய முதல்வராகத் தேர்வு செய்யப்பட்டார்.", "D. K. Shivakumar is selected")

# ------------------------------------------------------------------ June
other("ca-2026-06-05-vettri-thamizhagam", "2026-06-05", "TN",
      "Vijay's first Cabinet meeting approves 'Vettri Thamizhagam' vision with 436 schemes",
      "Chief Minister Vijay's first Cabinet meeting (5 June 2026) approved the 'Vettri Thamizhagam' vision document comprising 436 schemes across government departments (reports differ on whether it is 32 or 35 departments).",
      "விஜய் அமைச்சரவையின் முதல் கூட்டம்: 436 திட்டங்களுடன் 'வெற்றித் தமிழகம்' தொலைநோக்கு ஆவணம்",
      "முதல்வர் விஜய் தலைமையிலான அமைச்சரவையின் முதல் கூட்டத்தில் (2026 ஜூன் 5) அரசுத் துறைகள் முழுவதும் 436 திட்டங்களைக் கொண்ட 'வெற்றித் தமிழகம்' தொலைநோக்கு ஆவணத்துக்கு ஒப்புதல் அளிக்கப்பட்டது (துறைகளின் எண்ணிக்கை 32 அல்லது 35 என செய்திகள் வேறுபடுகின்றன).",
      "dtnext.in", "https://www.dtnext.in/news/tamilnadu/cm-vijay-unveils-436-schemes-for-tamil-nadus-development-during-first-cabinet-meeting")
wiki("ca-2026-06-15-lrlacm", "2026-06-15", "SCIENCE",
     "DRDO flight-tests Long Range Land Attack Cruise Missile",
     "DRDO successfully conducted the flight test of the Long Range Land Attack Cruise Missile (LRLACM) from Abdul Kalam Island, Odisha.",
     "நீண்டதூர தரைத்தாக்குதல் க்ரூஸ் ஏவுகணை சோதனை வெற்றி",
     "ஒடிசாவின் அப்துல் கலாம் தீவிலிருந்து நீண்டதூர தரைத்தாக்குதல் க்ரூஸ் ஏவுகணையை (LRLACM) DRDO வெற்றிகரமாகச் சோதித்தது.", "Long Range Land Attack Cruise Missile")
wiki("ca-2026-06-21-tiruvallur-gas", "2026-06-21", "TN",
     "Ammonia gas leak in Tiruvallur district kills 18",
     "An ammonia gas leak in Tiruvallur district, Tamil Nadu, left 18 people dead.",
     "திருவள்ளூர் மாவட்டத்தில் அமோனியா வாயுக் கசிவு: 18 பேர் உயிரிழப்பு",
     "தமிழ்நாட்டின் திருவள்ளூர் மாவட்டத்தில் ஏற்பட்ட அமோனியா வாயுக் கசிவில் 18 பேர் உயிரிழந்தனர்.", "ammonia gas leak")
wiki("ca-2026-06-30-army-chief", "2026-06-30", "POLITY",
     "General Dhiraj Seth takes charge as Chief of the Army Staff",
     "General Dhiraj Seth took charge as the Chief of the Army Staff.",
     "இராணுவத் தளபதியாக ஜெனரல் திராஜ் சேத் பொறுப்பேற்பு",
     "ஜெனரல் திராஜ் சேத் இராணுவத் தளபதியாகப் பொறுப்பேற்றார்.", "Dhiraj Seth")
wiki("ca-2026-06-30-tn-cow-slaughter", "2026-06-30", "TN",
     "Tamil Nadu government moves Supreme Court against Madras High Court cow-slaughter ban",
     "The Government of Tamil Nadu moved the Supreme Court against an order of the Madras High Court imposing a ban on the slaughter of cows in Tamil Nadu.",
     "பசு வதைத் தடை: மதராஸ் உயர் நீதிமன்ற உத்தரவுக்கு எதிராக தமிழக அரசு உச்ச நீதிமன்றத்தில் மனு",
     "தமிழ்நாட்டில் பசுக்களை வதைசெய்யத் தடை விதித்த மதராஸ் உயர் நீதிமன்ற உத்தரவுக்கு எதிராக தமிழக அரசு உச்ச நீதிமன்றத்தை அணுகியது.", "ban of slaughter of cows")

# ------------------------------------------------------------------ July
wiki("ca-2026-07-01-vb-g-ram-g", "2026-07-01", "INDIA",
     "VB-G RAM G law comes into effect, replacing MGNREGA",
     "The VB-G RAM G law came into effect, replacing the MGNREGA (rural employment guarantee scheme). The first instalment to states for implementation was released on 5 July.",
     "VB-G RAM G சட்டம் அமல்: MGNREGA-க்குப் பதிலாக",
     "கிராம வேலை உறுதித் திட்டமான MGNREGA-க்குப் பதிலாக VB-G RAM G சட்டம் அமலுக்கு வந்தது. செயல்படுத்துவதற்கான முதல் தவணை நிதி மாநிலங்களுக்கு ஜூலை 5 அன்று வழங்கப்பட்டது.", "VB G RAM G law")
wiki("ca-2026-07-06-waqf-mp", "2026-07-06", "POLITY",
     "Madhya Pradesh first to reconstitute Waqf Board under the Waqf (Amendment) Act, 2025",
     "Madhya Pradesh reconstituted its Waqf Board, including two Hindus, making it the first state to reorganise its Waqf Board under the Waqf (Amendment) Act, 2025.",
     "வக்ஃப் (திருத்த) சட்டம் 2025-இன் கீழ் வக்ஃப் வாரியத்தை மறுசீரமைத்த முதல் மாநிலம் மத்தியப் பிரதேசம்",
     "இரண்டு இந்துக்களையும் சேர்த்து மத்தியப் பிரதேசம் தனது வக்ஃப் வாரியத்தை மறுசீரமைத்து, வக்ஃப் (திருத்த) சட்டம் 2025-இன் கீழ் இவ்வாறு செய்த முதல் மாநிலமானது.", "reconstitutes the State's Waqf Board")
wiki("ca-2026-07-09-india-australia", "2026-07-09", "WORLD",
     "India and Australia sign defence and uranium export agreement",
     "India and Australia signed a defence and uranium export agreement.",
     "இந்தியா - ஆஸ்திரேலியா பாதுகாப்பு மற்றும் யுரேனியம் ஏற்றுமதி ஒப்பந்தம்",
     "இந்தியாவும் ஆஸ்திரேலியாவும் பாதுகாப்பு மற்றும் யுரேனியம் ஏற்றுமதி தொடர்பான ஒப்பந்தத்தில் கையெழுத்திட்டன.", "defence and uranium export")
other("ca-2026-07-19-fifa-world-cup", "2026-07-19", "SPORTS",
      "Spain win the 2026 FIFA World Cup",
      "The 2026 FIFA World Cup (11 June to 19 July) was won by Spain, their second title. It was the first World Cup hosted by three countries (USA, Mexico, Canada) and the first with 48 teams.",
      "2026 FIFA உலகக் கோப்பையை ஸ்பெயின் வென்றது",
      "2026 FIFA உலகக் கோப்பை (ஜூன் 11 - ஜூலை 19) போட்டியில் ஸ்பெயின் தனது இரண்டாவது கோப்பையை வென்றது. மூன்று நாடுகள் (அமெரிக்கா, மெக்சிகோ, கனடா) இணைந்து நடத்திய முதல் உலகக் கோப்பை இது; 48 அணிகள் பங்கேற்ற முதல் உலகக் கோப்பையும் இதுவே.",
      "Wikipedia '2026 FIFA World Cup'", "https://en.wikipedia.org/wiki/2026_FIFA_World_Cup")
other("ca-2026-07-23-cwg-glasgow", "2026-07-23", "SPORTS",
      "Commonwealth Games 2026 begin in Glasgow",
      "The XXIII Commonwealth Games were held in Glasgow, Scotland, from 23 July to 2 August 2026. India sent 122 athletes in ten sports (its 19th appearance).",
      "2026 காமன்வெல்த் விளையாட்டுகள் - கிளாஸ்கோ",
      "XXIII காமன்வெல்த் விளையாட்டுப் போட்டிகள் 2026 ஜூலை 23 முதல் ஆகஸ்ட் 2 வரை ஸ்காட்லாந்தின் கிளாஸ்கோவில் நடைபெற்றன. இந்தியா பத்து விளையாட்டுகளில் 122 வீரர்களை அனுப்பியது (19-வது பங்கேற்பு).",
      "Wikipedia 'India at the 2026 Commonwealth Games'", "https://en.wikipedia.org/wiki/India_at_the_2026_Commonwealth_Games")
wiki("ca-2026-07-24-us-tariff", "2026-07-24", "ECONOMY",
     "US imposes a 10% tariff on India citing forced labour in supply chains",
     "The United States imposed a 10% tariff on India, citing the alleged presence of forced labour in the supply chain.",
     "கட்டாயத் தொழிலைக் காரணம் காட்டி இந்தியா மீது 10% வரி விதித்த அமெரிக்கா",
     "விநியோகச் சங்கிலியில் கட்டாயத் தொழில் இருப்பதாகக் கூறி அமெரிக்கா இந்தியா மீது 10% வரி விதித்தது.", "10% tariff on India")
wiki("ca-2026-07-25-education-minister", "2026-07-25", "POLITY",
     "Dharmendra Pradhan resigns as Education Minister; Pralhad Joshi replaces him",
     "Dharmendra Pradhan resigned as Union Education Minister amid outrage over the NEET controversy, and was replaced by Pralhad Joshi.",
     "கல்வி அமைச்சர் தர்மேந்திர பிரதான் ராஜினாமா; பிரஹ்லாத் ஜோஷி புதிய அமைச்சர்",
     "NEET சர்ச்சையால் எழுந்த கண்டனத்தை அடுத்து மத்தியக் கல்வி அமைச்சர் தர்மேந்திர பிரதான் ராஜினாமா செய்தார்; பிரஹ்லாத் ஜோஷி அவருக்குப் பதிலாகப் பொறுப்பேற்றார்.", "Dharmendra Pradhan resigns")
wiki("ca-2026-07-25-sarnath", "2026-07-25", "AWARDS",
     "Sarnath designated a UNESCO World Heritage Site",
     "The ancient Buddhist site of Sarnath was designated a UNESCO World Heritage Site.",
     "சாரநாத் யுனெஸ்கோ உலகப் பாரம்பரியத் தலமாக அறிவிப்பு",
     "பழமையான பௌத்தத் தலமான சாரநாத் யுனெஸ்கோ உலகப் பாரம்பரியத் தலமாக அறிவிக்கப்பட்டது.", "Sarnath")
wiki("ca-2026-07-30-paper-leak-bill", "2026-07-30", "POLITY",
     "Parliament passes the Anti Paper Leak Bill, 2026",
     "Parliament passed the Anti Paper Leak Bill 2026 by voice vote. The Prime Minister had said the draft law would include fast-track courts and strict punishment for those involved in paper leaks.",
     "வினாத்தாள் கசிவுத் தடுப்பு மசோதா 2026 நாடாளுமன்றத்தில் நிறைவேற்றம்",
     "வினாத்தாள் கசிவுத் தடுப்பு மசோதா 2026-ஐ நாடாளுமன்றம் குரல் வாக்கெடுப்பில் நிறைவேற்றியது. வினாத்தாள் கசிவில் ஈடுபடுவோருக்கு விரைவு நீதிமன்றங்களும் கடுமையான தண்டனையும் இருக்கும் என பிரதமர் முன்பு தெரிவித்திருந்தார்.", "Anti Paper Leak Bill")
wiki("ca-2026-07-31-gsi-dg", "2026-07-31", "SCIENCE",
     "Varsha Ashok Aglawe becomes first woman Director General of the Geological Survey of India",
     "Varsha Ashok Aglawe assumed charge as the 54th Director General of the Geological Survey of India, the first woman to hold the post.",
     "இந்திய புவியியல் ஆய்வுத் துறையின் முதல் பெண் தலைமை இயக்குநர்",
     "வர்ஷா அசோக் அக்லாவே இந்திய புவியியல் ஆய்வுத் துறையின் 54-வது தலைமை இயக்குநராகப் பொறுப்பேற்றார்; இப்பதவியை வகிக்கும் முதல் பெண் இவர்.", "Varsha Ashok Aglawe")

# ------------------------------------------------------------------ August
wiki("ca-2026-08-01-tilak-award", "2026-08-01", "AWARDS",
     "Lokmanya Tilak National Award presented to NSA Ajit Doval",
     "Home Minister Amit Shah presented the Lokmanya Tilak National Award to National Security Advisor Ajit Doval.",
     "தேசிய பாதுகாப்பு ஆலோசகர் அஜித் தோவலுக்கு லோகமான்ய திலக் தேசிய விருது",
     "உள்துறை அமைச்சர் அமித் ஷா தேசிய பாதுகாப்பு ஆலோசகர் அஜித் தோவலுக்கு லோகமான்ய திலக் தேசிய விருதை வழங்கினார்.", "Lokmanya Tilak National Award")
wiki("ca-2026-08-01-bhogapuram-airport", "2026-08-01", "INDIA",
     "Alluri Sitarama Raju International Airport inaugurated at Bhogapuram",
     "Prime Minister Narendra Modi inaugurated the Alluri Sitarama Raju International Airport at Bhogapuram, Andhra Pradesh.",
     "போகாபுரத்தில் அல்லூரி சீதாராம ராஜு சர்வதேச விமான நிலையம் திறப்பு",
     "ஆந்திரப் பிரதேசத்தின் போகாபுரத்தில் அல்லூரி சீதாராம ராஜு சர்வதேச விமான நிலையத்தை பிரதமர் மோடி திறந்து வைத்தார்.", "Alluri Sitarama Raju International Airport")
wiki("ca-2026-08-13-sangeet-natak", "2026-08-13", "AWARDS",
     "President presents Sangeet Natak Akademi Fellowships and Awards for 2024 and 2025",
     "President Droupadi Murmu presented the Sangeet Natak Akademi Fellowships and Awards for 2024 and 2025.",
     "சங்கீத நாடக அகாடமி ஃபெல்லோஷிப், விருதுகள் (2024, 2025) வழங்கல்",
     "குடியரசுத் தலைவர் திரௌபதி முர்மு 2024, 2025-ஆம் ஆண்டுகளுக்கான சங்கீத நாடக அகாடமி ஃபெல்லோஷிப்புகளையும் விருதுகளையும் வழங்கினார்.", "Sangeet Natak Akademi")
wiki("ca-2026-08-21-isro-launch", "2026-08-21", "SCIENCE",
     "IN-SPACe chief says ISRO will stop making launch vehicles",
     "IN-SPACe head Pawan Goenka said that ISRO is going to stop making launch vehicles and that this will be done by the private sector.",
     "ஏவுகலங்கள் தயாரிப்பை ISRO நிறுத்தி தனியார் துறை மேற்கொள்ளும் - IN-SPACe தலைவர்",
     "ஏவுகலங்களைத் தயாரிப்பதை ISRO நிறுத்தும் என்றும் அதைத் தனியார் துறை மேற்கொள்ளும் என்றும் IN-SPACe தலைவர் பவன் கோயங்கா தெரிவித்தார்.", "ISRO is going to stop making launch vehicles")
wiki("ca-2026-08-26-drdo-private", "2026-08-26", "SCIENCE",
     "Defence Minister permits transfer of DRDO missile technologies to private firms",
     "Defence Minister Rajnath Singh permitted the transfer of DRDO missile technologies to Indian private companies.",
     "DRDO ஏவுகணைத் தொழில்நுட்பங்களை தனியார் நிறுவனங்களுக்கு மாற்ற அனுமதி",
     "DRDO ஏவுகணைத் தொழில்நுட்பங்களை இந்தியத் தனியார் நிறுவனங்களுக்கு மாற்ற பாதுகாப்பு அமைச்சர் ராஜ்நாத் சிங் அனுமதி அளித்தார்.", "transfer of DRDO missile technologies")

# ------------------------------------------------------------------ September - November
wiki("ca-2026-09-12-brics", "2026-09-12", "WORLD",
     "18th BRICS summit held in New Delhi (12-13 September)",
     "New Delhi hosted the 18th BRICS summit on 12-13 September 2026.",
     "புது தில்லியில் 18-வது BRICS உச்சி மாநாடு (செப்டம்பர் 12-13)",
     "18-வது BRICS உச்சி மாநாடு 2026 செப்டம்பர் 12-13 தேதிகளில் புது தில்லியில் நடைபெற்றது.", "18th BRICS summit")
wiki("ca-2026-09-30-pahalgam-mastermind", "2026-09-30", "INDIA",
     "Pahalgam attack mastermind Hashim Moosa killed in encounter",
     "Hashim Moosa, a top Lashkar-e-Taiba commander described as the mastermind of the 2025 Pahalgam attack, was killed in an encounter with security forces in Budgam, Jammu and Kashmir.",
     "பஹல்காம் தாக்குதல் சூத்திரதாரி ஹஷிம் மூசா என்கவுன்டரில் உயிரிழப்பு",
     "2025 பஹல்காம் தாக்குதலின் சூத்திரதாரியாகக் கருதப்படும் லஷ்கர்-இ-தொய்பா மூத்த தளபதி ஹஷிம் மூசா ஜம்மு-காஷ்மீரின் புட்காமில் பாதுகாப்புப் படையினருடனான மோதலில் கொல்லப்பட்டார்.", "Hashim Moosa")
other("ca-2026-10-04-asian-games", "2026-10-04", "SPORTS",
      "Asian Games 2026 (Aichi-Nagoya): India finish fourth with 85 medals",
      "At the XX Asian Games in Aichi-Nagoya, Japan (19 September to 4 October 2026), India (495 athletes, 36 sports) finished fourth with 85 medals: 21 gold, 27 silver and 37 bronze, its second-highest Asian Games tally after 2022. Flag-bearers: Manu Bhaker and Pawan Sehrawat (opening), Neeru Dhanda (closing).",
      "2026 ஆசிய விளையாட்டுகள் (ஐச்சி-நகோயா): இந்தியா 85 பதக்கங்களுடன் 4-வது இடம்",
      "ஜப்பானின் ஐச்சி-நகோயாவில் நடந்த XX ஆசிய விளையாட்டுகளில் (2026 செப்டம்பர் 19 - அக்டோபர் 4) இந்தியா (495 வீரர்கள், 36 விளையாட்டுகள்) 21 தங்கம், 27 வெள்ளி, 37 வெண்கலம் என 85 பதக்கங்களுடன் நான்காம் இடம் பிடித்தது; 2022-க்குப் பின் இரண்டாவது அதிகபட்சப் பதக்க எண்ணிக்கை இது. தொடக்க விழாவில் மனு பாக்கர், பவன் செஹ்ராவத், நிறைவு விழாவில் நீரு தண்டா கொடி ஏந்தினர்.",
      "Wikipedia 'India at the 2026 Asian Games'", "https://en.wikipedia.org/wiki/India_at_the_2026_Asian_Games")
other("ca-2026-10-09-nobel-peace", "2026-10-09", "AWARDS",
      "Nobel Peace Prize 2026 awarded to Navi Pillay",
      "The Norwegian Nobel Committee announced on 9 October 2026 that the Nobel Peace Prize 2026 goes to South African jurist Navi Pillay (the prize is to be presented on 10 December in Oslo).",
      "2026 அமைதிக்கான நோபல் பரிசு நவி பிள்ளைக்கு",
      "2026 அமைதிக்கான நோபல் பரிசு தென்னாப்பிரிக்க சட்ட நிபுணர் நவி பிள்ளைக்கு வழங்கப்படும் என நார்வே நோபல் குழு 2026 அக்டோபர் 9 அன்று அறிவித்தது (டிசம்பர் 10 அன்று ஓஸ்லோவில் வழங்கப்படும்).",
      "Wikipedia '2026 Nobel Peace Prize'", "https://en.wikipedia.org/wiki/2026_Nobel_Peace_Prize")
wiki("ca-2026-10-20-nz-fta-effect", "2026-10-20", "ECONOMY",
     "New Zealand-India Free Trade Agreement comes into effect",
     "The New Zealand-India Free Trade Agreement, signed on 28 April 2026, came into effect.",
     "நியூசிலாந்து - இந்தியா தடையற்ற வர்த்தக ஒப்பந்தம் அமலுக்கு வந்தது",
     "2026 ஏப்ரல் 28 அன்று கையெழுத்தான நியூசிலாந்து - இந்தியா தடையற்ற வர்த்தக ஒப்பந்தம் அமலுக்கு வந்தது.", "New Zealand–India Free Trade Agreement comes into effect")
wiki("ca-2026-10-25-weightlifting", "2026-10-25", "SPORTS",
     "Ahmedabad to host the 2029 World Weightlifting Championships",
     "The International Weightlifting Federation awarded the hosting rights for the 2029 World Weightlifting Championships to Ahmedabad.",
     "2029 உலக பளுதூக்கும் சாம்பியன்ஷிப் அகமதாபாத்தில்",
     "2029 உலக பளுதூக்கும் சாம்பியன்ஷிப் போட்டியை நடத்தும் உரிமையை சர்வதேச பளுதூக்கும் கூட்டமைப்பு அகமதாபாதுக்கு வழங்கியது.", "2029 World Weightlifting Championships")
wiki("ca-2026-11-04-us-sanctions", "2026-11-04", "WORLD",
     "US imposes new sanctions on India-based entities linked to Iran",
     "The United States imposed new sanctions on companies and individuals in India with ties to the Iranian regime.",
     "ஈரானுடன் தொடர்புடைய இந்திய நிறுவனங்கள் மீது அமெரிக்கா புதிய தடைகள்",
     "ஈரான் அரசுடன் தொடர்புடைய இந்திய நிறுவனங்கள், தனிநபர்கள் மீது அமெரிக்கா புதிய தடைகளை விதித்தது.", "new sanctions on companies and individuals in India")
other("ca-2026-11-03-tn-age-relaxation", "2026-11-03", "TN",
      "REPORT: Tamil Nadu CM announces 5-year age relaxation for government job aspirants",
      "A Business Standard report dated 3 November 2026 says Chief Minister C. Joseph Vijay announced a five-year age relaxation for government job aspirants appearing in competitive examinations. We could not find the official order; check the current TNPSC notification and the government order before relying on this for eligibility.",
      "செய்தி: அரசுப் பணி தேர்வர்களுக்கு 5 ஆண்டு வயது தளர்வு என முதல்வர் அறிவிப்பு (உறுதிசெய்யப்படவில்லை)",
      "போட்டித் தேர்வு எழுதும் அரசுப் பணி ஆர்வலர்களுக்கு ஐந்து ஆண்டு வயது தளர்வு வழங்கப்படும் என முதல்வர் சி. ஜோசப் விஜய் அறிவித்ததாக 2026 நவம்பர் 3 தேதியிட்ட பிசினஸ் ஸ்டாண்டர்டு செய்தி தெரிவிக்கிறது. அதிகாரப்பூர்வ அரசாணையை எங்களால் காண முடியவில்லை; தகுதிக்கு இதை நம்புவதற்கு முன் தற்போதைய TNPSC அறிவிப்பையும் அரசாணையையும் சரிபார்க்கவும்.",
      "business-standard.com (news report; official order not found)", "https://www.business-standard.com/topic/tamil-nadu-government/page-5")

if __name__ == "__main__":
    out = sys.argv[1]
    if ERRORS:
        print("\n".join(ERRORS)); sys.exit(1)
    ids = [i["id"] for i in ITEMS]
    assert len(ids) == len(set(ids)), "duplicate ids"
    ITEMS.sort(key=lambda i: i["date"], reverse=True)
    pack = {
        "version": 1,
        "dataUpTo": "2026-11-17",
        "_notice": ("Compiled on 17 Nov 2026 from Wikipedia's '2026 in India' article (CC BY-SA; each entry links the news report that "
                    "article cites) plus a few other cited reports and Wikipedia event articles. Items are written in our own words, "
                    "are NOT independently verified, and are far from complete. Tamil text is a draft translation. Check anything "
                    "important against an official source. IDs are stable: never renumber, only add."),
        "items": ITEMS,
    }
    with open(out, "w", encoding="utf-8") as f:
        json.dump(pack, f, ensure_ascii=False, indent=2)
        f.write("\n")
    from collections import Counter
    print(len(ITEMS), "items;", dict(Counter(i["category"] for i in ITEMS)))
    print("months:", sorted(Counter(i["date"][:7] for i in ITEMS).items()))
