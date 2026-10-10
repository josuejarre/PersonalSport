# Project Plan

Quiero hacer una aplicacion de deportes donde se sincronice con mi huawei wacht gt para que tome los datos de los deportes que he realizado segun el reloj, haga un segumiento de mi perdida de grasa y ganacia muscular y me haga tablas de ejercicios para correr, ir a la piscina y hacer ejercicios musculares. tambien debe permitirme añadir las maquinas que tengo en el gimnasio y que me haga tablas de ejercicios. Debe tambien tener registros de natacion al igual que de carreras (cortas medias y largas) y debe permitir introducirlo tanto manual como por medio de los registros del reloj. En los dias de musculacion debe tomar los resultados y debe tambien permitir tomar fotos para comparar los progresos y saber los objetivos. Incluir catálogo de máquinas de Technogym, Life Fitness, Matrix, Fit4Life, Oss Fitness, BH Fitness, Bodytone, Salter, Etenon, Decathlon y Cecotec. Sección de contratación de entrenador personal para futura app web. Módulo de atletismo con series/intervalos. Módulo de recomendación y scraping de suplementación de calidad más barata. Algoritmo de IA local para generación de rutinas adaptadas y análisis inteligente del progreso físico.

## Project Brief

# Project Brief: PersonalSport (MVP)

## Features

1. **Multi-Sport & Activity Tracking (Huawei Watch GT & Manual)**
   - Tracks running (short, middle, and long-distance), swimming, and athletics (interval and series workouts) via Huawei Watch GT synchronization or manual entry.

2. **On-Device AI Routine Generator & Equipment Catalog**
   - Uses a local AI algorithm to create tailored training routines based on user goals and selected equipment from a catalog (Technogym, Life Fitness, BH Fitness, Matrix, BH, Decathlon, etc.).

3. **Smart Physical Progress & Visual Body Analytics**
   - Tracks body composition metrics (fat loss, muscle gain) and strength performance using local AI analysis, supported by visual photo progress comparisons.

4. **Supplementation & Personal Trainer Hub**
   - Recommends personalized supplementation with price-to-quality search/scraping, alongside a booking portal for hiring personal trainers (prepared for web integration).

## High-Level Tech Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Navigation Architecture:** Jetpack Navigation 3 (State-driven)
- **Adaptive Strategy:** Compose Material Adaptive Library
- **Async & State Management:** Kotlin Coroutines, Flow / StateFlow, ViewModel
- **On-Device Intelligence:** Local AI / ML Engine (for privacy-first, on-device workout generation & analytics)

## Implementation Steps
**Total Duration:** 19h 35m 52s

### Task_1_DataLayer: Set up Room Database, entities (Equipment, WorkoutLog, BodyMetric, ProgressPhoto), and repositories for the PersonalSport app.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - Room DB builds successfully
  - Entities and DAOs for Equipment, WorkoutLog, BodyMetric, ProgressPhoto exist
  - Repositories handle basic CRUD operations
- **Duration:** 5m 14s

### Task_2_WearableSync: Implement Health Connect / Wearable sync to import Huawei Watch GT, Amazfit, Xiaomi, and smart scales data.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - Health API integrated
  - Permissions configured correctly
  - Sync mechanism reads and saves wearable data to local Room DB
- **Duration:** 19m 6s

### Task_3_UI_Navigation: Build adaptive UI with Jetpack Compose & Navigation 3 (Dashboard, Equipment Catalog with major brands, Workout Planner, Manual Entry).
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - Navigation 3 state-driven flows working
  - Compose Material Adaptive layouts used
  - Screens for Dashboard, Planner, Equipment, and Manual Entry implemented
- **Duration:** 18h 53m 44s

### Task_4_CameraProgress: Integrate CameraX for capturing progress photos and UI for visual comparison, saving photo URIs to Room.
- **Status:** COMPLETED
- **Acceptance Criteria:**
  - CameraX implemented successfully
  - User can capture and save progress photos
  - UI to compare historical progress photos is functional
- **Duration:** 7m 1s

### Task_5_LocalAI_And_Athletics: Implement Local AI Engine for personalized routine generation (including athletics series & intervals) and intelligent body progress analysis.
- **Status:** COMPLETED
- **Updates:** Implemented LocalAIFitnessEngine for on-device intelligent workout routine generation and body progress analytics. Added support for Athletics interval series (400m, 1000m, pyramid, fartlek, sprints) and integrated AI Progress Insights Card on the DashboardScreen. All 13 unit tests passed and assembleDebug built cleanly.
- **Acceptance Criteria:**
  - Local AI engine generates custom routines based on user equipment and goals
  - Athletics interval/series workouts included
  - Local AI analyzes fat loss and muscle gain trends

### Task_6_Supplementation_And_Trainer: Implement Supplementation hub (AI recommendation + price comparison/scraping) and Personal Trainer booking portal for future web integration.
- **Status:** COMPLETED
- **Updates:** Implemented Supplementation Hub with AI recommendations and live vendor price comparison engine (Prozis, HSN, MyProtein, Decathlon, Bulk). Implemented Personal Trainer Directory with certified coaches, subscription plans (Single Session, Monthly, Quarterly), appointment booking, chat preview, and web deep link interoperability (`https://personalsport.app/trainers/{id}/hire?plan={planId}`). Added routes to AppNavigationSuite and shortcuts on DashboardScreen. `./gradlew assembleDebug` and unit tests passed.
- **Acceptance Criteria:**
  - Supplement recommendation module active
  - Price comparison/scraping interface implemented
  - Personal Trainer directory with subscription plans and web integration link ready
- **Duration:** 7m 52s

### Task_7_Run_And_Verify: Run and Verify app stability with critic_agent. Confirm alignment with user requirements and zero crashes.
- **Status:** COMPLETED
- **Updates:** Critic agent performed full QA verification. All unit tests passed, app compiles cleanly with zero crashes, all user requirements (wearable sync, local AI engine, athletics series, gym catalog, progress photos, supplement scraping, trainer directory, adaptive UI) are fully implemented and verified.
- **Acceptance Criteria:**
  - App builds and runs without crashing
  - All unit tests pass
  - Features align with user requirements
- **Duration:** 2m 55s

