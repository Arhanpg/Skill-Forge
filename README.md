
<div align="center">

![Skill-Forge Banner](https://img.shields.io/badge/Skill--Forge-v1.0-blue?style=for-the-badge)
[![Android](https://img.shields.io/badge/Android-API%2024+-green?style=for-the-badge&logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-purple?style=for-the-badge&logo=kotlin)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)](LICENSE)

**Transform Your Learning Journey Into an Epic RPG Adventure**

*Turn the grind of deliberate practice into an addictive heroic progression loop*

[Features](#-features) • [Screenshots](#-screenshots) • [Installation](#-installation) • [Tech Stack](#-tech-stack) • [Contributing](#-contributing)

</div>

---

## 📖 About

**Skill-Forge** is a revolutionary mobile RPG that gamifies cognitive skill mastery in Coding, Mathematics, and Logic. Unlike generic habit trackers, Skill-Forge directly translates focused study time into character progression, turning monotonous academic practice into an engaging, rewarding experience.

### 🎯 The Problem We Solve

STEM students and self-learners often struggle with:
- **Invisible Progress**: Traditional study methods lack immediate feedback
- **Motivation Loss**: The disconnect between effort and reward leads to burnout
- **Dishonest Tracking**: Generic to-do lists are easy to skip without accountability

### ✨ Our Solution

Skill-Forge bridges the gap between study and gaming through:
- **Real-World Stat Mapping**: Level up specific skills like (Coding, Math, Problem Solving) by logging focused time
- **Boss Battle Verification**: AI-generated quizzes ensure honest progress tracking
- **Visual Skill Trees**: Node-based graphs showcasing intellectual growth
- **Edu Coins Economy**: Earn rewards redeemable for courses and Amazon coupons

---

## 🚀 Features

### Core Gameplay



#### ⏱️ Focus Timer Engine
- Robust Pomodoro-style timer for focused study sessions
- Automatic XP and Edu Coins calculation
- Do Not Disturb mode integration for distraction-free learning

#### 👹 Boss Battle System
- **Fundamental Quiz**: Tests basics of completed tasks (available for 24 hours)
- **Advanced Quiz**: Unlocks 3 days after Fundamental quiz with adaptive difficulty
- AI-powered quiz generation based on your task descriptions
- Must score 70%+ to conquer the boss and earn rewards

#### 🌳 Visual Skill Tree
- Cyber-Fantasy aesthetic with glowing nodes
- Track your intellectual growth through an interactive graph
- Unlock new abilities and milestones as you progress

#### 🛒 Inventory & Shop
- Spend Edu Coins on cosmetic upgrades
- Unlock visual enhancements
- Future: Redeem for educational courses and Amazon vouchers

### Engagement Features

#### 📊 Daily Quests
- Streak mechanics with daily challenges
- "Code for 30 mins today" style achievements
- Consistent rewards to build study habits

#### 🎮 Gamification Elements
- Level-up animations and sound effects
- XP progress bars on every screen
- Satisfying visual feedback for every action
- Dark mode UI with neon RPG elements

#### 📈 Premium Features (Quest Master Subscription)
- **Unlimited Task Tracking**: Free tier limited to 3 active tasks
- **Advanced Analytics**: Peak performance hours and skill decay charts
- **Ad-Free Experience**: Remove all advertisements
- Price: ₹149/month

---

## 📸 Screenshots


![IMG-20251203-WA0015](https://github.com/user-attachments/assets/356bcb09-f9da-4ba5-9ed1-57c9e2d22d38)

![IMG-20251203-WA0014](https://github.com/user-attachments/assets/cb413d80-6e75-4756-ae26-757ba69f8e11)

![IMG-20251203-WA0013](https://github.com/user-attachments/assets/2ddf9d25-a328-4b09-a7b4-f437cf093af4)
![IMG-20251203-WA0012](https://github.com/user-attachments/assets/f3fb1544-b36e-4c62-8552-994bb733b2f6)
![IMG-20251203-WA0017](https://github.com/user-attachments/assets/f03a05e7-d1e1-4ba0-a9f3-a3911f0a11a1)
![IMG-20251203-WA0016](https://github.com/user-attachments/assets/518d8b8d-60ba-41dd-9b18-ca723f3d2333)


## 🛠️ Tech Stack

### Frontend
- **Framework**: Jetpack Compose (Modern Android UI) 
- **Architecture**: MVVM (Model-View-ViewModel)
- **Language**: Kotlin

### Backend & Services
- **Backend**: Firebase Studio
  - Firestore: Cloud database for user data
  - Firebase Auth: Secure authentication
  - Cloud Functions: Anti-cheat logic and serverless operations 
- **AI Integration**: Gemini API for quiz generation and task tracking
- **Monetization**: Google Mobile Ads SDK (AdMob)

### Key Libraries & Tools
- **Jetpack Components**: Navigation, ViewModel, LiveData
- **Dependency Injection**: Hilt/Dagger
- **Networking**: Retrofit, OkHttp
- **Coroutines**: Asynchronous programming
- **Material Design 3**: Modern UI components

---

## 📦 Installation

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 or higher
- Android SDK API 24+
- Active internet connection for Firebase

### Setup Instructions

1. **Clone the Repository**
```bash
git clone https://github.com/Arhanpg/Skill-Forge.git
cd Skill-Forge
```

2. **Configure Firebase**
- Create a Firebase project at [Firebase Console](https://console.firebase.google.com)
- Download `google-services.json` and place it in the `app/` directory
- Enable Firestore, Authentication, and Cloud Functions

3. **Add Gemini API Key**
Create a `local.properties` file in the root directory:
```properties
GEMINI_API_KEY=your_gemini_api_key_here
```

4. **Add AdMob Configuration**
Update `AndroidManifest.xml` with your AdMob App ID:
```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-4021531566495189~XXXXXXXXXX"/>
```

5. **Build and Run**
```bash
./gradlew assembleDebug
```

Or use Android Studio's "Run" button.

---

## 🎮 How to Use

### Getting Started

1. **Create Your Hero**
   - Launch the app and create your avatar
   - Choose your starting specialization

2. **Start Your First Quest**
   - Tap "Start Quest" on the home screen
   - Select a skill (Coding, Math, Problem Solving)
   - Create a task with a detailed description
   - Set your focus timer (recommended: 25-45 minutes)

3. **Focus & Complete**
   - Study during the timer countdown
   - Enable DND mode for best results
   - Complete your task before time runs out

4. **Conquer the Boss**
   - Take the Fundamental Quiz within 24 hours
   - Score 70%+ to defeat the boss
   - Earn XP, level up, and collect Edu Coins
   - Advanced Quiz unlocks after 3 days

5. **Grow Your Skills**
   - Watch your Skill Tree expand
   - Purchase upgrades in the Shop
   - Complete Daily Quests for bonuses
   - Track your progress through Analytics

---

## 💰 Monetization

### Free Tier
- Full access to core features
- Up to 3 active tasks
- Banner ads on passive screens
- Optional rewarded video ads for bonuses

### Rewarded Ads
- **Focus Boost**: 1.5x XP multiplier (watch before session)
- **Second Wind**: Retry failed boss without cooldown
- User-initiated, never intrusive
- Limited per session to maintain quality

### Quest Master Subscription (₹149/mo)
- Unlimited task tracking
- Advanced analytics dashboard
- Ad-free experience
- Priority support

---

## 🗺️ Roadmap

### Phase 1: MVP (Current)
- [x] Hero creation and stat system
- [x] Focus timer with XP calculation
- [x] AI-powered Boss Battle quizzes
- [x] Basic Skill Tree visualization
- [x] Inventory and cosmetic shop

### Phase 2: Engagement (Next 3 Months)
- [ ] Daily Quest system
- [ ] Streak mechanics and rewards
- [ ] Enhanced animations and sound effects
- [ ] Social sharing for Boss victories
- [ ] Edu Coins redemption system

### Phase 3: Community (6 Months)
- [ ] Skill-Grid: Connect with users having complementary skills
- [ ] Project collaboration features
- [ ] Leaderboards and achievements
- [ ] Content marketplace for custom skill templates

### Phase 4: Expansion (12 Months)
- [ ] Additional skill categories
- [ ] Guilds and multiplayer features
- [ ] Advanced course integrations
- [ ] Web dashboard for desktop tracking

---

## 🤝 Contributing

We welcome contributions from the community! Here's how you can help:

### Ways to Contribute

1. **Report Bugs**: Open an issue with detailed reproduction steps
2. **Suggest Features**: Share your ideas in the Discussions tab
3. **Submit Pull Requests**: Fix bugs or add features
4. **Improve Documentation**: Help make our docs clearer
5. **Share Feedback**: Tell us about your experience

### Development Guidelines

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Follow Kotlin coding conventions
4. Write meaningful commit messages
5. Add tests for new functionality
6. Submit a Pull Request with detailed description

### Code Style
- Follow [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Comment complex logic
- Keep functions small and focused

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👥 Team A³

- **Arhan Ghosarwade** - Primary Contact & Lead Developer
- Email: arhanghosarwade05@gmail.com
- AdMob Publisher ID: pub-4021531566495189

---

## 🙏 Acknowledgments

- Google AdMob for monetization support
- Firebase for backend infrastructure
- Gemini AI for intelligent quiz generation
- The Android developer community
- All our beta testers and early adopters

---

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/Arhanpg/Skill-Forge/issues)
- **Email**: arhanghosarwade05@gmail.com
- **Documentation**: [Wiki](https://github.com/Arhanpg/Skill-Forge/wiki)

---

## 🌟 Show Your Support

If Skill-Forge helps you master your skills, give it a ⭐️!

<div align="center">

**Level Up Your Learning. Forge Your Future.**

Made with ❤️ by Team A³

</div>
