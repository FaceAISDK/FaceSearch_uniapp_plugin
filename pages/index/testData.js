/**
 * testData.js
 * Optimized: Generates test data programmatically instead of hardcoding strings.
 */

// 1. Define reusable constants to save space
const BASE_FEATURE = "zfCMvEdMijxXUje8ClGQvMREvrwK1Zm97h67ORP1k72muTC9xvcRPvu1sjmjL6u5XgSDO_E1UTy7gdO6VBLNvVOkQrsMI9W6B8Ttuh0IOLy1Oo68JkLvPA9D1r0v18Q793xLvamqrTw-2le8kZsWPeghZTw3CzU8V_-DPGLSWb6iaCG9pZ7Fu4DxW75Ltkm9Ma6xulFUcbwjW6I6haAxPQ0_7LgcjsO3sEbAOX5A9btsUyA7DI4EPTpbPj5QnQs-h3xkOydpa71AKUW6GlP8O-g6q725mkE7_bpXPYRzYzvmKwG9BdhjO3UDTr3RVBM8NmaCPEDDCr3yyMI9v8edPvwnCrzgD_M9JpRLOiMy-bweYNQ6KoOTuwHbVry6K9q-N6ILPlkigLoBFL89Bs40PKxQ37tTeAU7Kh7gPXdvUT0vZEa5FRVUvXAzhrxzHeg9lrwwvhvjCDsyn8c7si_lPSZu-73tuPO8S4IkviYVDTwEGIK6xGE9u_Pfnjwp8Fg86QYLPYHqqD3mPLa7vicEvTUpyTrqi5I7cBwlvGVY3DqHozE8kO5IOxkwwb3TUum5-N6XvKieljyXmy07x_y4OypoVTq_yns65bVJu2xNS740_GM8SlfbuiuP2D1ov829X2rKvH9j5rsD_-E7eTI6OybYmroWN4O7A4GSO3MyZjuNlds80t8FvpaZlzupgVU85e70uwePFDxnrYS9XGTnu4l3r715aLS9y7QUvEguNTsqGvE7Ki-4uuvVOTrAbvW91l2JvQz0e72_0aA7hlBDvCMdeLtIe_m70SaouzKAkb0u7de9nsS0vNygJDxPvGq6nmCXuhFphLunzx-9Sxadu3tzy7vJ3Vk7K8bwOiXVB7twkQE6FAJIOW23mbsCI1Q9m4meu3EqnbqOEHa-lQ6mPWJ4irvEafE9NuKVvdhZmLu1Sfq8dHiKvTD1vrv6_wq8FqspvdNLEz65vak7PqXoO4Lz5b1xzcw9VhHOPQZ7iD36c-Q94rhSPLz05bzG3UG7";

// 2. Define the static/edge-case data first as real JS objects
const rawData = [
    { 
        faceID: "User_1001", 
        tag: "vip", 
        group: "group_a", 
        faceFeature: BASE_FEATURE, 
        updateTime: 1704067200000 
    },
    { 
        faceID: "User_1002", 
        faceFeature: "AAAAPwAAAL8AAIA_AACAvw" 
    },
    { 
        faceID: "User_1003", 
        tag: "", 
        group: "", 
        faceFeature: "zcyMP83MDEAzM1NA", 
        updateTime: 0, 
        extraField: "should_be_ignored" 
    },
    { 
        tag: "invalid_no_id", 
        group: "test", 
        faceFeature: "AAAAAA", 
        updateTime: 1704067200000 
    },
    { 
        faceID: "User_1005", 
        tag: "bad_feature_data", 
        faceFeature: "ThisIsNotBase64!!!", 
        updateTime: 1704067200000 
    }
];

// 3. Programmatically generate the repetitive batch data (user_2000 to user_2045)
const START_TIME = 1704067200000;
for (let i = 0; i <= 45; i++) {
    rawData.push({
        faceID: `user_${2000 + i}`,
        tag: "batch_gen",
        group: "test_group",
        // Note: Your original data prepended "Test-12345" to the base feature
        faceFeature: "Test-12345" + BASE_FEATURE, 
        updateTime: START_TIME + (i * 1000) // Increments by 1 second per user
    });
}

// 4. Export as a JSON string
export const JSON_FACE_FEATURES_DATA = JSON.stringify(rawData);