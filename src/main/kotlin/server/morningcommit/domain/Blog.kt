package server.morningcommit.domain

enum class Blog(val displayName: String) {
    // Korean Tech Company Blogs
    KAKAO_TECH("Kakao Tech"),
    TOSS_TECH("Toss Tech"),
    WOOWA_BROS("Woowa Bros"),
    NAVER_D2("Naver D2"),
    LINE_ENGINEERING("LINE Engineering"),
    HYPERCONNECT_TECH("Hyperconnect Tech"),
    DEVSISTERS_TECH("Devsisters Tech"),
    BANKSALAD_TECH("Banksalad Tech"),

    // Global Tech Company Blogs
    NETFLIX_TECH("Netflix Tech"),
    GITHUB_BLOG("GitHub Blog"),
    AIRBNB_TECH("Airbnb Tech"),
    SPOTIFY_ENGINEERING("Spotify Engineering")
}
