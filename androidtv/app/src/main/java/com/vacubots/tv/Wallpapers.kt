package com.vacubots.tv

import android.content.Context
import android.graphics.*
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.view.View
import android.widget.ImageView
import kotlin.math.*

/**
 * The animated wallpapers behind the interface. Each one is a still picture with a few moving parts laid over it, all driven by the
 * clock (nothing is stored or allocated per frame): falling snow or leaves, twinkling lights and stars, drifting haze and mist, and
 * gentle ripples in water and grass. The pictures are 1672 x 941 and are cropped to fill the screen. Every moving layer redraws on the
 * shared clock in Effects.kt (30 a second, together, and standing still while a dialog has the focus).
 */
object Wallpapers {
    class Warp(val mask: Int, val kind: Int, val ground: Float, val cloud: Float)   // kind: 0 water, 1 wind in grass, 2 falling water, 3 clouds only
    class Theme(val id: String, val name: String, val img: Int, val top: Int, val bottom: Int, val warp: Warp?, val effects: (Context) -> List<View>)

    const val IMG_W = 1672f
    const val IMG_H = 941f
    private val BULBS = floatArrayOf(0.799f,0.9171f,1.00f,0.8547f,0.5898f,1.00f,0.2614f,0.8597f,1.00f,0.7967f,0.9044f,1.00f,0.9217f,0.7311f,1.00f,0.317f,0.7811f,1.00f,0.2644f,0.8852f,1.00f,0.2751f,0.7906f,1.00f,0.4982f,0.746f,1.00f,0.4892f,0.7418f,0.99f,0.4432f,0.5951f,0.99f,0.8589f,0.8034f,0.99f,0.2488f,0.8544f,0.99f,0.7769f,0.797f,0.99f,0.9121f,0.7832f,0.99f,0.515f,0.7524f,0.99f,0.1029f,0.3921f,0.99f,0.3415f,0.5388f,0.99f,0.0951f,0.4548f,0.99f,0.701f,0.9416f,0.99f,0.1591f,0.7513f,0.99f,0.8798f,0.5622f,0.99f,0.2805f,0.8874f,0.99f,0.9007f,0.7768f,0.99f,0.7566f,0.7014f,0.99f,0.8858f,0.9968f,0.99f,0.2913f,0.882f,0.99f,0.3301f,0.8077f,0.99f,0.6866f,0.7885f,0.99f,0.3014f,0.8735f,0.99f,0.5604f,0.6918f,0.99f,0.5526f,0.6908f,0.99f,0.1322f,0.7439f,0.99f,0.7536f,0.9437f,0.98f,0.8206f,0.9182f,0.98f,0.1513f,0.7481f,0.98f,0.4468f,0.7301f,0.98f,0.8307f,0.9022f,0.98f,0.1681f,0.7503f,0.98f,0.3941f,0.6642f,0.98f,0.945f,0.7301f,0.98f,0.3535f,0.8119f,0.98f,0.7309f,0.949f,0.98f,0.3577f,0.3571f,0.98f,0.9821f,0.7736f,0.98f,0.3403f,0.7864f,0.98f,0.5772f,0.6876f,0.98f,0.7416f,0.9437f,0.98f,0.131f,0.526f,0.98f,0.195f,0.7906f,0.98f,0.3923f,0.8321f,0.97f,0.9994f,0.7545f,0.97f,0.1962f,0.6663f,0.97f,0.9551f,0.7566f,0.97f,0.924f,0.4155f,0.97f,0.2392f,0.8746f,0.97f,0.3714f,0.8257f,0.97f,0.4701f,0.729f,0.97f,0.488f,0.4708f,0.97f,0.2255f,0.7758f,0.97f,0.2333f,0.7588f,0.97f,0.4121f,0.4867f,0.97f,0.4749f,0.7418f,0.97f,0.1507f,0.5898f,0.96f,0.3672f,0.3486f,0.96f,0.3947f,0.746f,0.96f,0.2243f,0.678f,0.96f,0.2937f,0.4867f,0.96f,0.805f,0.7928f,0.96f,0.1866f,0.796f,0.96f,0.7961f,0.7917f,0.95f,0.4587f,0.7258f,0.95f,0.3493f,0.4091f,0.95f,0.6699f,0.9564f,0.95f,0.8487f,0.6738f,0.95f,0.8451f,0.356f,0.95f,0.183f,0.779f,0.94f,0.0658f,0.4038f,0.94f,0.7536f,0.6801f,0.94f,0.5514f,0.6238f,0.94f,0.4617f,0.7396f,0.94f,0.8319f,0.7386f,0.94f,0.0789f,0.492f,0.94f,0.6705f,0.627f,0.94f,0.4904f,0.7046f,0.93f,0.3624f,0.5239f,0.93f,0.372f,0.5388f,0.93f,0.3923f,0.3688f,0.93f,0.985f,0.322f,0.93f,0.4438f,0.7535f,0.93f,0.4318f,0.7577f,0.93f,0.0933f,0.6918f,0.93f,0.1734f,0.6057f,0.93f,0.3565f,0.5377f,0.93f,0.2165f,0.8565f,0.93f,0.5024f,0.6823f,0.92f,0.1411f,0.6993f,0.92f,0.2422f,0.7928f,0.92f,0.3953f,0.4825f,0.92f,0.9611f,0.2242f,0.92f,0.2566f,0.6355f,0.92f,0.0867f,0.3985f,0.92f,0.3726f,0.4368f,0.92f,0.2823f,0.4963f,0.91f,0.3864f,0.3316f,0.91f,0.5401f,0.5207f,0.91f,0.5353f,0.5632f,0.91f,0.1663f,0.6397f,0.91f,0.0963f,0.3135f,0.91f,0.8014f,0.5919f,0.91f,0.8714f,0.5749f,0.90f,0.0257f,0.3762f,0.90f,0.07f,0.3826f,0.90f,0.2273f,0.8066f,0.90f,0.433f,0.5696f,0.90f,0.1806f,0.7396f,0.90f,0.3026f,0.5898f,0.90f,0.2165f,0.8278f,0.89f,0.9575f,0.3369f,0.89f,0.9049f,0.7503f,0.89f,0.9091f,0.5452f,0.89f,0.6938f,0.627f,0.89f,0.3786f,0.6748f,0.89f,0.4641f,0.7577f,0.89f,0.0825f,0.6791f,0.89f,0.2614f,0.6196f,0.89f,0.3995f,0.5494f,0.89f,0.7524f,0.899f,0.89f,0.134f,0.6982f,0.89f,0.4844f,0.678f,0.89f,0.1065f,0.7173f,0.89f,0.2279f,0.7003f,0.89f,0.8834f,0.0f,0.89f,0.6633f,0.9416f,0.89f,0.8224f,0.7535f,0.89f,0.5102f,0.6578f,0.88f,0.2667f,0.6408f,0.88f,0.0909f,0.3751f,0.88f,0.305f,0.9001f,0.88f,0.1513f,0.7747f,0.87f)
    private val STARRY = floatArrayOf(0.3242f,0.1509f,0.41f,0.0227f,0.2635f,0.41f,0.2207f,0.0106f,0.40f,0.0233f,0.1764f,0.39f,0.0592f,0.559f,0.37f,0.9641f,0.0361f,0.37f,0.8134f,0.1095f,0.36f,0.0203f,0.0446f,0.34f,0.8923f,0.1116f,0.33f,0.0048f,0.102f,0.33f,0.5712f,0.2795f,0.32f,0.0383f,0.0733f,0.32f,0.2446f,0.5749f,0.31f,0.0957f,0.1275f,0.31f,0.366f,0.1849f,0.30f,0.8553f,0.0499f,0.30f,0.991f,0.3114f,0.29f,0.6328f,0.5377f,0.29f,0.8577f,0.17f,0.29f,0.7255f,0.4527f,0.29f,0.9803f,0.3783f,0.29f,0.0789f,0.0521f,0.29f,0.128f,0.101f,0.28f,0.1603f,0.1902f,0.28f,0.0526f,0.0213f,0.28f,0.113f,0.3762f,0.28f,0.927f,0.3018f,0.27f,0.3511f,0.1435f,0.27f,0.3403f,0.2806f,0.27f,0.9934f,0.118f,0.27f,0.3535f,0.0106f,0.27f,0.8648f,0.0797f,0.26f,0.7057f,0.0149f,0.26f,0.5299f,0.204f,0.26f,0.1722f,0.1934f,0.26f,0.9258f,0.1498f,0.25f,0.552f,0.3603f,0.25f,0.1465f,0.2965f,0.25f,0.4294f,0.0914f,0.25f,0.2303f,0.2391f,0.25f,0.2709f,0.1254f,0.25f,0.5072f,0.2455f,0.25f,0.64f,0.1158f,0.24f,0.9067f,0.153f,0.24f,0.4342f,0.3039f,0.24f,0.9629f,0.1339f,0.24f,0.2967f,0.0882f,0.24f,0.0239f,0.0106f,0.24f,0.4013f,0.4814f,0.24f,0.4803f,0.1594f,0.24f,0.1382f,0.3571f,0.24f,0.2835f,0.5707f,0.23f,0.2685f,0.4421f,0.23f,0.8266f,0.1934f,0.23f,0.9348f,0.2338f,0.23f,0.2303f,0.0223f,0.23f,0.0203f,0.0744f,0.23f,0.2105f,0.0648f,0.23f,0.3941f,0.0202f,0.23f,0.1041f,0.3645f,0.23f,0.4109f,0.5579f,0.23f,0.759f,0.0553f,0.23f,0.6382f,0.4867f,0.23f,0.2476f,0.1722f,0.23f,0.3038f,0.3528f,0.23f,0.4617f,0.2136f,0.22f,0.2841f,0.2104f,0.22f,0.2781f,0.3518f,0.22f,0.1292f,0.4857f,0.22f,0.613f,0.2848f,0.22f)
    private val MILKYWAY = floatArrayOf(0.9892f,0.8714f,0.50f,0.9713f,0.1998f,0.50f,0.9707f,0.8937f,0.50f,0.9486f,0.8990f,0.50f,0.9438f,0.6610f,0.50f,0.9360f,0.8406f,0.50f,0.9342f,0.4963f,0.50f,0.9336f,0.9330f,0.50f,0.9264f,0.6111f,0.50f,0.9246f,0.8172f,0.50f,0.9240f,0.2731f,0.50f,0.9139f,0.4346f,0.50f,0.9103f,0.5537f,0.50f,0.9097f,0.8310f,0.50f,0.8744f,0.8215f,0.50f,0.8726f,0.8757f,0.50f,0.8678f,0.9235f,0.50f,0.8600f,0.3358f,0.50f,0.8583f,0.4580f,0.50f,0.8230f,0.3348f,0.50f,0.8152f,0.8672f,0.50f,0.8056f,0.9798f,0.50f,0.7931f,0.8863f,0.50f,0.7853f,0.1541f,0.50f,0.7291f,0.0383f,0.50f,0.7123f,0.5813f,0.50f,0.6746f,0.5919f,0.50f,0.6041f,0.9394f,0.50f,0.5987f,0.3082f,0.50f,0.5748f,0.7821f,0.50f,0.5646f,0.2104f,0.50f,0.5598f,0.3337f,0.50f,0.5490f,0.0531f,0.50f,0.5263f,0.0457f,0.50f,0.4988f,0.0840f,0.50f,0.4743f,0.9745f,0.50f,0.4557f,0.2434f,0.50f,0.4539f,0.1934f,0.50f,0.4336f,0.3018f,0.50f,0.4300f,0.4006f,0.50f,0.4252f,0.9851f,0.50f,0.4228f,0.3518f,0.50f,0.4049f,0.3624f,0.50f,0.4037f,0.4888f,0.50f,0.3762f,0.8172f,0.50f,0.3714f,0.2997f,0.50f,0.3433f,0.7365f,0.50f,0.3349f,0.1286f,0.50f,0.3128f,0.3039f,0.50f,0.3128f,0.0510f,0.50f,0.3014f,0.4612f,0.50f,0.2955f,0.9224f,0.50f,0.2937f,0.2625f,0.50f,0.2763f,0.8034f,0.50f,0.2644f,0.4431f,0.50f,0.2548f,0.5739f,0.50f,0.2303f,0.8799f,0.50f,0.2135f,0.3581f,0.50f,0.2123f,0.7492f,0.50f,0.2105f,0.6408f,0.50f,0.2093f,0.8693f,0.50f,0.2016f,0.0510f,0.50f,0.1986f,0.5186f,0.50f,0.1986f,0.1923f,0.50f,0.1890f,0.7290f,0.50f,0.1842f,0.0191f,0.50f,0.1836f,0.5898f,0.50f,0.1639f,0.5484f,0.50f,0.1591f,0.9362f,0.50f,0.1334f,0.6302f,0.50f,0.1268f,0.9033f,0.50f,0.1232f,0.0064f,0.50f,0.1160f,0.5930f,0.50f,0.1136f,0.3889f,0.50f,0.1059f,0.4580f,0.50f,0.0795f,0.1743f,0.50f,0.0718f,0.0680f,0.50f,0.0586f,0.2912f,0.50f,0.0580f,0.6504f,0.50f,0.0449f,0.5654f,0.50f,0.0329f,0.3135f,0.50f,0.0245f,0.1424f,0.50f,0.0239f,0.0542f,0.50f,0.0108f,0.1998f,0.50f,0.9348f,0.1955f,0.50f,0.9145f,0.8661f,0.50f,0.8744f,0.3709f,0.50f,0.8289f,0.9352f,0.50f,0.7285f,0.0861f,0.50f,0.6346f,0.1413f,0.50f)
    // Found in the new stadium picture (stars, the nine brightest, the four floodlight clusters, the small lights along the far stand): x, y as a share of the picture, then a strength.
    private val STADIUM_STARS = floatArrayOf(0.6250f,0.0117f,0.4857f,0.3804f,0.0170f,0.3196f,0.8487f,0.0213f,0.2661f,0.1705f,0.0244f,0.2893f,0.4910f,0.0255f,0.4536f,0.6077f,0.0276f,0.4536f,0.6142f,0.0298f,0.4875f,0.5120f,0.0308f,0.4589f,0.7901f,0.0351f,0.3964f,0.3864f,0.0361f,0.4875f,0.7249f,0.0436f,0.2857f,0.7380f,0.0457f,0.3482f,0.7016f,0.0510f,0.4036f,0.4300f,0.0531f,0.3643f,0.3726f,0.0574f,0.2643f,0.8911f,0.0574f,0.3357f,0.8714f,0.0595f,0.4929f,0.3750f,0.0616f,0.4196f,0.4982f,0.0638f,0.4929f,0.5245f,0.0638f,0.3214f,0.3583f,0.0659f,0.3625f,0.4701f,0.0659f,0.4107f,0.3307f,0.0670f,0.3804f,0.5754f,0.0670f,0.2804f,0.4587f,0.0755f,0.2929f,0.5760f,0.0755f,0.3536f,0.7362f,0.0755f,0.3429f,0.5227f,0.0765f,0.3054f,0.6495f,0.0765f,0.2518f,0.9701f,0.0776f,0.3446f,0.4533f,0.0871f,0.2661f,0.6693f,0.0956f,0.4732f,0.7225f,0.0967f,0.2768f,0.8008f,0.0967f,0.4911f,0.3684f,0.0978f,0.4625f,0.5090f,0.0988f,0.4357f,0.6328f,0.0999f,0.2911f,0.2470f,0.1031f,0.2821f,0.6782f,0.1041f,0.4875f,0.8421f,0.1063f,0.3179f,0.6298f,0.1073f,0.3196f,0.6705f,0.1084f,0.3607f,0.4007f,0.1105f,0.4411f,0.4707f,0.1233f,0.2518f,0.6112f,0.1371f,0.3786f,0.9904f,0.1424f,0.3286f,0.7321f,0.1467f,0.3250f,0.3236f,0.1520f,0.4518f,0.9850f,0.1594f,0.3089f,0.7087f,0.1668f,0.2750f,0.9725f,0.1700f,0.4839f,0.9031f,0.1743f,0.4571f,0.2739f,0.1796f,0.2768f,0.6794f,0.1807f,0.3089f,0.7673f,0.1838f,0.4339f,0.2961f,0.1849f,0.3768f,0.6782f,0.1849f,0.2786f,0.5933f,0.1860f,0.3714f,0.6112f,0.1892f,0.2536f,0.7554f,0.1913f,0.4893f,0.9844f,0.1934f,0.2518f,0.5658f,0.1945f,0.2536f,0.6591f,0.1955f,0.3821f,0.4001f,0.1977f,0.4857f,0.7650f,0.1977f,0.4250f,0.5939f,0.2104f,0.2982f,0.5963f,0.2104f,0.3500f,0.6328f,0.2125f,0.2536f,0.6986f,0.2125f,0.3036f,0.7614f,0.2125f,0.2518f,0.6663f,0.2157f,0.2518f,0.3565f,0.2221f,0.4607f,0.9151f,0.2604f,0.4429f,0.6160f,0.2667f,0.4875f,0.9306f,0.2710f,0.4518f,0.3409f,0.2827f,0.4125f,0.7339f,0.2997f,0.4768f,0.7063f,0.3007f,0.2679f,0.6083f,0.3039f,0.4821f,0.5951f,0.3103f,0.3054f,0.8660f,0.3156f,0.4875f,0.8935f,0.3156f,0.3268f,0.5646f,0.3337f,0.2768f,0.6752f,0.3358f,0.4643f,0.7267f,0.3369f,0.4821f,0.4055f,0.3379f,0.4518f,0.5532f,0.3379f,0.2929f,0.7518f,0.3379f,0.4714f,0.8487f,0.3464f,0.4857f,0.7482f,0.3560f,0.2732f,0.5150f,0.3571f,0.4839f,0.7339f,0.3624f,0.3571f,0.7219f,0.3688f,0.4804f,0.7596f,0.3698f,0.2804f,0.9360f,0.3836f,0.4857f,0.8266f,0.3900f,0.2518f,0.9486f,0.3985f,0.3536f,0.5024f,0.4028f,0.3464f,0.7805f,0.4038f,0.4107f,0.6459f,0.4091f,0.3875f,0.4354f,0.4240f,0.3518f,0.7177f,0.4357f,0.2589f,0.7022f,0.4559f,0.4464f,0.5891f,0.4761f,0.2893f,0.8182f,0.4782f,0.2839f,0.5179f,0.4803f,0.2554f,0.5209f,0.4835f,0.2768f,0.9199f,0.4867f,0.3125f,0.7961f,0.5271f,0.4804f,0.9839f,0.5834f,0.4946f,0.9809f,0.5909f,0.4929f)
    private val STADIUM_SPARKS = floatArrayOf(0.9839f,0.5834f,0.4982f,0.0638f,0.9809f,0.5909f,0.8714f,0.0595f,0.8008f,0.0967f,0.7554f,0.1913f,0.6142f,0.0298f,0.6782f,0.1041f,0.8660f,0.3156f)
    private val STADIUM_LAMPS = floatArrayOf(0.0060f,0.2335f,0.0882f,0.3066f,0.1871f,0.4055f,0.2809f,0.5056f)
    private val STADIUM_RIM = floatArrayOf(0.9713f,0.6079f,0.3000f,0.9629f,0.6100f,0.3000f,0.9426f,0.6121f,0.3000f,0.9342f,0.6153f,0.3000f,0.9498f,0.6153f,0.3000f,0.4175f,0.6164f,0.3000f,0.9061f,0.6164f,0.3000f,0.9067f,0.6164f,0.3000f,0.9181f,0.6174f,0.3000f,0.9133f,0.6185f,0.3000f,0.8403f,0.6238f,0.3000f,0.8409f,0.6238f,0.3000f,0.8565f,0.6238f,0.3000f,0.8146f,0.6249f,0.3000f,0.7877f,0.6259f,0.3000f,0.4593f,0.6270f,0.3000f,0.4306f,0.6281f,0.3000f,0.4916f,0.6281f,0.3000f,0.7273f,0.6281f,0.3000f,0.6579f,0.6291f,0.3000f,0.5257f,0.6302f,0.3000f,0.5903f,0.6302f,0.3000f,0.6585f,0.6302f,0.3000f,0.6758f,0.6302f,0.3000f,0.6938f,0.6302f,0.3000f,0.4755f,0.6323f,0.3000f,0.5694f,0.6323f,0.3000f,0.6196f,0.6323f,0.3000f,0.6406f,0.6323f,0.3000f,0.9737f,0.6387f,0.3000f,0.9743f,0.6387f,0.3000f,0.9749f,0.6387f,0.3000f,0.9725f,0.6397f,0.3000f,0.9677f,0.6408f,0.3000f,0.9533f,0.6419f,0.3000f,0.9569f,0.6429f,0.3000f,0.9402f,0.6440f,0.3000f,0.8642f,0.6514f,0.3000f)

    /** Maps a point in the 1672 x 941 picture to the screen, the way center-crop fills it. */
    class Crop(w: Float, h: Float) {
        val s = max(w / IMG_W, h / IMG_H)
        val ox = (w - IMG_W * s) / 2f
        val oy = (h - IMG_H * s) / 2f
        fun x(nx: Float) = nx * IMG_W * s + ox
        fun y(ny: Float) = ny * IMG_H * s + oy
    }

    private fun hash(i: Int, k: Int): Float = ((((i + 1) * 7919L + 13) * (k + 3) * 2654435761L) ushr 8 and 0xFFFF).toFloat() / 65535f
    private fun now() = SystemClock.uptimeMillis() / 1000f

    /** The picture itself, with the water rippled by a few pixels (water only: the edges and the clouds stay still). A mesh over the whole picture, pushed about only where the mask says there is water, grass or cloud. */
    class WallpaperView(ctx: Context, private val bmp: Bitmap, mask: Bitmap, private val kind: Int, private val ground: Float, private val cloud: Float) : EffectView(ctx) {
        private val cols = 48
        private val rows = 27
        private val n = (cols + 1) * (rows + 1)
        private val verts = FloatArray(n * 2)
        private val gAmp = FloatArray(n)
        private val cAmp = FloatArray(n)
        init {
            for (j in 0..rows) for (i in 0..cols) {
                val px = mask.getPixel((i * (mask.width - 1)) / cols, (j * (mask.height - 1)) / rows)
                gAmp[j * (cols + 1) + i] = Color.red(px) / 255f
                cAmp[j * (cols + 1) + i] = Color.green(px) / 255f
            }
        }
        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            if (w <= 0f) return
            val bw = bmp.width.toFloat(); val bh = bmp.height.toFloat()
            val s = max(w / bw, h / bh); val ox = (w - bw * s) / 2f; val oy = (h - bh * s) / 2f
            val u = w / 1920f
            val t = now()
            for (j in 0..rows) for (i in 0..cols) {
                val k = j * (cols + 1) + i
                val nx = i / cols.toFloat(); val ny = j / rows.toFloat()
                var dx = 0f; var dy = 0f
                val ga = gAmp[k]
                if (ga > 0.03f) {
                    val a = ga * ground * u
                    when (kind) {
                        0 -> { dx = a * (sin(ny * 90f + t * 1.7f + nx * 6f) + 0.5f * sin(ny * 190f - t * 2.3f + nx * 13f)); dy = a * 0.5f * sin(nx * 70f + ny * 30f + t * 1.3f) }
                        1 -> { dx = a * sin(nx * 9f + ny * 5f - t * 1.1f) * (0.6f + 0.4f * sin(t * 0.37f + nx * 3f)) }
                        2 -> { dy = a * sin(ny * 130f - t * 3.2f + nx * 8f); dx = a * 0.4f * sin(nx * 60f + t * 1.1f) }
                    }
                }
                if (i == 0 || i == cols || j == 0 || j == rows) { dx = 0f; dy = 0f }   // the edges never move, so no black shows at the sides
                verts[k * 2] = nx * bw * s + ox + dx
                verts[k * 2 + 1] = ny * bh * s + oy + dy
            }
            canvas.drawBitmapMesh(bmp, cols, rows, verts, 0, null, 0, null)
        }
    }

    /** Moving light on the water and grass instead of bending the picture (which tore its edges and dragged the rocks along): small glints that brighten and fade at spots the mask marks as water or grass. The picture itself never moves. kind 0 water glints, 1 light rolling over grass, 2 streaks falling down a waterfall. */
    class Shimmer(ctx: Context, mask: Bitmap, private val kind: Int, count: Int) : EffectView(ctx) {
        private val glow = glowBitmap()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = PorterDuffColorFilter(if (kind == 1) 0xFFFFF0C0.toInt() else 0xFFFFFFFF.toInt(), PorterDuff.Mode.SRC_IN)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        }
        private val dst = RectF()
        private val xs: FloatArray
        private val ys: FloatArray
        init {
            val cells = ArrayList<Int>()
            for (j in 0 until mask.height) for (i in 0 until mask.width) if (Color.red(mask.getPixel(i, j)) > 140) cells.add(j * mask.width + i)
            val m = minOf(count, cells.size)
            xs = FloatArray(m); ys = FloatArray(m)
            for (k in 0 until m) {
                val cell = cells[(hash(k, 11) * (cells.size - 1)).toInt()]
                xs[k] = ((cell % mask.width) + hash(k, 12)) / mask.width
                ys[k] = ((cell / mask.width) + hash(k, 13)) / mask.height
            }
        }
        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            if (w <= 0f) return
            val c = Crop(w, h); val u = w / 1920f; val t = now()
            for (k in xs.indices) {
                var x = c.x(xs[k]); var y = c.y(ys[k])
                val cyc = 3.5f + 4f * hash(k, 1)
                val ph = ((t + hash(k, 2) * cyc) % cyc) / cyc
                val env = sin(ph * 3.1416f)
                var rx: Float; var ry: Float; var a: Float
                when (kind) {
                    0 -> { rx = u * (22f + 30f * hash(k, 3)); ry = u * (2.5f + 2f * hash(k, 4)); x += (ph - 0.5f) * u * 24f; a = 0.55f * env * env }
                    1 -> { rx = u * (70f + 60f * hash(k, 3)); ry = u * (22f + 14f * hash(k, 4)); x += (ph - 0.5f) * u * 120f; a = 0.16f * env }
                    else -> { rx = u * (2.5f + 2f * hash(k, 3)); ry = u * (26f + 26f * hash(k, 4)); y += ph * u * 70f; a = 0.5f * env }
                }
                paint.alpha = (255 * a).toInt().coerceIn(0, 255)
                dst.set(x - rx, y - ry, x + rx, y + ry)
                canvas.drawBitmap(glow, null, dst, paint)
            }
        }
    }

    /** A soft round glow, used for bulbs, stars, haze and mist. */
    private fun glowBitmap(): Bitmap {
        val b = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(48f, 48f, 48f, intArrayOf(0xFFFFFFFF.toInt(), 0x66FFFFFF, 0x00FFFFFF), floatArrayOf(0f, 0.35f, 1f), Shader.TileMode.CLAMP) }
        Canvas(b).drawCircle(48f, 48f, 48f, p)
        return b
    }

    /** Points on the picture that glow (Christmas bulbs, stars). Most hold steady; some slowly dim and brighten, each on its own cycle. */
    class Sparkles(ctx: Context, private val pts: FloatArray, private val color: Int, private val twinkleEvery: Int, private val base: Float, private val peak: Float, private val size: Float, private val period: Float) : EffectView(ctx) {
        private val glow = glowBitmap()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN); xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD) }
        private val dst = RectF()
        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            if (w <= 0f) return
            val c = Crop(w, h); val t = now(); val u = w / 1920f
            for (i in 0 until pts.size / 3) {
                val x = c.x(pts[i * 3]); val y = c.y(pts[i * 3 + 1]); val strength = pts[i * 3 + 2]
                var a = base
                if (i % twinkleEvery == 0) {
                    val cyc = period * (0.7f + 0.9f * hash(i, 1))
                    a = base + (peak - base) * (0.5f + 0.5f * sin(t * 6.2832f / cyc + hash(i, 2) * 6.2832f))
                }
                val r = u * size * (0.7f + 0.8f * strength) * (1f + 0.35f * (a - base))
                paint.alpha = (255 * a).toInt().coerceIn(0, 255)
                dst.set(x - r, y - r, x + r, y + r)
                canvas.drawBitmap(glow, null, dst, paint)
            }
        }
    }

    /** Snow in three depths: tiny slow flakes far away, middle ones, and a few large soft ones close up, all leaning with one wind. */
    class Snow(ctx: Context, private val wind: Float = 1f) : EffectView(ctx) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        private val counts = intArrayOf(60, 28, 6)
        private val sizes = floatArrayOf(1.6f, 3.4f, 11f)
        private val falls = floatArrayOf(46f, 26f, 15f)
        private val alphas = intArrayOf(150, 205, 70)
        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            if (w <= 0f) return
            val u = w / 1920f; val t = now()
            for (layer in 0..2) for (i in 0 until counts[layer]) {
                val id = layer * 1000 + i
                val size = u * sizes[layer] * (0.8f + 0.5f * hash(id, 1))
                val fall = falls[layer] * (0.85f + 0.3f * hash(id, 2))
                val prog = ((t + hash(id, 3) * fall) % fall) / fall
                val y = -size * 3 + prog * (h + size * 6)
                val drift = wind * u * (14f + 9f * layer) * t
                val span = w + size * 8
                var x = (hash(id, 4) * w + drift) % span - size * 4
                if (x < -size * 4) x += span
                x += sin((t * (0.35f + 0.4f * hash(id, 5)) + hash(id, 6) * 6.28f).toDouble()).toFloat() * u * (8f + 6f * layer)
                val a = (alphas[layer] * (0.7f + 0.3f * hash(id, 7))).toInt()
                if (layer < 2) { paint.alpha = a / 3; canvas.drawCircle(x, y, size * 1.8f, paint) }
                paint.alpha = a; canvas.drawCircle(x, y, size, paint)
            }
        }
    }

    /** Slow soft haze: Mars dust blowing along the ground, mist rising off the falls, nebula wisps, clouds. kind 0 dust, 1 mist, 2 nebula, 3 clouds. */
    class Haze(ctx: Context, private val kind: Int) : EffectView(ctx) {
        private val glow = glowBitmap()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val dst = RectF()
        private val n = if (kind == 1) 10 else if (kind == 2) 7 else if (kind == 4) 8 else 9
        private val colors = when (kind) { 0 -> intArrayOf(0xFFE8B48A.toInt(), 0xFFD99A70.toInt()); 1 -> intArrayOf(0xFFFFFFFF.toInt()); 2 -> intArrayOf(0xFFFF7ACC.toInt(), 0xFF7A9CFF.toInt(), 0xFFB07AFF.toInt()); 4 -> intArrayOf(0xFF9AA8C4.toInt()); else -> intArrayOf(0xFFFFFFFF.toInt()) }
        private val filters = colors.map { PorterDuffColorFilter(it, PorterDuff.Mode.SRC_IN) }   // made once; it used to be made again for every haze patch on every frame
        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            if (w <= 0f) return
            val c = Crop(w, h); val t = now(); val u = w / 1920f
            for (i in 0 until n) {
                paint.colorFilter = filters[i % filters.size]
                var x: Float; var y: Float; var rx: Float; var ry: Float; var a: Float
                when (kind) {
                    0 -> { rx = u * (380f + 260f * hash(i, 1)); ry = rx * 0.22f; val sp = u * (14f + 18f * hash(i, 2)); val span = w + rx * 3
                        x = (hash(i, 3) * span + sp * t) % span - rx * 1.5f; y = c.y(0.60f + 0.32f * hash(i, 4)); a = 0.20f }
                    1 -> { rx = u * (150f + 110f * hash(i, 1)); ry = rx * 0.9f; val life = 16f + 8f * hash(i, 2); val p = ((t + hash(i, 3) * life) % life) / life
                        x = c.x(0.38f + 0.30f * hash(i, 4)) + sin((t * 0.3f + hash(i, 5) * 6f).toDouble()).toFloat() * u * 40f + p * u * 60f; y = c.y(0.80f) - p * IMG_H * c.s * 0.30f
                        a = 0.32f * sin(p * 3.1416f) }
                    2 -> { rx = w * (0.18f + 0.14f * hash(i, 1)); ry = rx * 0.7f; x = c.x(0.15f + 0.7f * hash(i, 2)) + sin((t * 0.06f + hash(i, 3) * 6f).toDouble()).toFloat() * w * 0.05f
                        y = c.y(0.15f + 0.7f * hash(i, 4)) + cos((t * 0.05f + hash(i, 5) * 6f).toDouble()).toFloat() * h * 0.04f; a = 0.22f + 0.12f * sin(t * 0.25f + hash(i, 6) * 6f) }
                    4 -> { rx = u * (360f + 260f * hash(i, 1)); ry = rx * 0.34f; val sp = u * (5f + 7f * hash(i, 2)); val span = w + rx * 3
                        x = (hash(i, 3) * span + sp * t) % span - rx * 1.5f; y = c.y(0.04f + 0.36f * hash(i, 4)); a = 0.26f }
                    else -> { rx = u * (300f + 240f * hash(i, 1)); ry = rx * 0.38f; val sp = u * (14f + 16f * hash(i, 2)); val span = w + rx * 3
                        x = (hash(i, 3) * span + sp * t) % span - rx * 1.5f; y = c.y(0.05f + 0.85f * hash(i, 4)); a = 0.30f }
                }
                paint.alpha = (255 * a).toInt().coerceIn(0, 255)
                dst.set(x - rx, y - ry, x + rx, y + ry)
                canvas.drawBitmap(glow, null, dst, paint)
            }
        }
    }

    /** A faint meteor streak, once in a long while. */
    class Meteor(ctx: Context, private val period: Float = 53f) : EffectView(ctx) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            if (w <= 0f) return
            val t = now(); val cycle = (t / period).toInt(); val p = (t % period) / 1.1f
            if (p in 0f..1f) {
                val c = Crop(w, h); val u = w / 1920f
                val x0 = c.x(0.25f + 0.6f * hash(cycle, 1)); val y0 = c.y(0.06f + 0.22f * hash(cycle, 2))
                val len = u * 190f; val dx = -len * 0.82f; val dy = len * 0.57f
                val hx = x0 + dx * p * 2.2f; val hy = y0 + dy * p * 2.2f
                paint.shader = LinearGradient(hx, hy, hx - dx, hy - dy, 0xCCFFFFFF.toInt(), 0x00FFFFFF, Shader.TileMode.CLAMP)
                paint.alpha = (255 * sin(p * 3.1416f)).toInt(); paint.strokeWidth = u * 2.4f
                canvas.drawLine(hx, hy, hx - dx, hy - dy, paint)
            }
        }
    }

    /** Slow floating specks (pollen, seeds) that drift up and across and sway as they go. */
    class Motes(ctx: Context, private val color: Int, private val n: Int) : EffectView(ctx) {
        private val glow = glowBitmap()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN); xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD) }
        private val dst = RectF()
        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            if (w <= 0f) return
            val u = w / 1920f; val t = now()
            for (i in 0 until n) {
                val life = 22f + 20f * hash(i, 1); val p = ((t + hash(i, 2) * life) % life) / life
                val x = hash(i, 3) * w + p * u * 160f + sin((t * (0.4f + 0.5f * hash(i, 4)) + hash(i, 5) * 6.28f).toDouble()).toFloat() * u * 30f
                val y = h * (0.95f - 0.85f * p)
                val r = u * (5f + 8f * hash(i, 6)); val a = 0.8f * sin(p * 3.1416f) * (0.5f + 0.5f * sin(t * 1.3f + hash(i, 7) * 6.28f))
                paint.alpha = (255 * a).toInt().coerceIn(0, 255)
                dst.set(x - r, y - r, x + r, y + r)
                canvas.drawBitmap(glow, null, dst, paint)
            }
        }
    }

    /** A few butterflies wandering slowly over the meadow, wings opening and closing. */
    class Butterflies(ctx: Context) : EffectView(ctx) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val cols = intArrayOf(0xFFFFE27A.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFB56B.toInt(), 0xFFF7A8D0.toInt(), 0xFFFFE27A.toInt())
        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            if (w <= 0f) return
            val u = w / 1920f; val t = now()
            for (i in 0 until 5) {
                val sp = 0.045f + 0.03f * hash(i, 1)
                val x = w * (0.5f + 0.42f * sin((t * sp + hash(i, 2) * 6.28f).toDouble()).toFloat() + 0.05f * sin((t * 0.31f + i).toDouble()).toFloat())
                val y = h * (0.58f + 0.25f * hash(i, 3)) + h * 0.07f * sin((t * (0.17f + 0.1f * hash(i, 4)) * 2f + hash(i, 5) * 6.28f).toDouble()).toFloat()
                val dir = if (cos((t * sp + hash(i, 2) * 6.28f).toDouble()) > 0) 1f else -1f
                val flap = abs(sin((t * (7f + 3f * hash(i, 6)) + i).toDouble())).toFloat()
                val size = u * (9f + 4f * hash(i, 7))
                val a = 200
                paint.color = cols[i]; paint.alpha = a
                canvas.save(); canvas.translate(x, y); canvas.rotate(dir * -8f)
                canvas.drawOval(-size * (0.15f + 0.85f * flap), -size * 0.9f, 0f, size * 0.35f, paint)
                canvas.drawOval(0f, -size * 0.9f, size * (0.15f + 0.85f * flap), size * 0.35f, paint)
                paint.color = 0xFF3A2A1A.toInt(); paint.alpha = 220
                canvas.drawOval(-size * 0.07f, -size * 0.7f, size * 0.07f, size * 0.55f, paint)
                canvas.restore()
            }
        }
    }

    /** Light rain: thin slanted streaks falling fast, in two depths. */
    class Rain(ctx: Context) : EffectView(ctx) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFCFE0F5.toInt(); strokeCap = Paint.Cap.ROUND }
        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            if (w <= 0f) return
            val u = w / 1920f; val t = now()
            for (i in 0 until 38) {
                val far = i % 4 != 0
                val len = u * (if (far) 12f else 22f) * (0.8f + 0.4f * hash(i, 1))
                val dur = if (far) 1.5f else 1.1f
                val p = ((t + hash(i, 2) * dur) % dur) / dur
                val x = hash(i, 3) * (w + h * 0.25f) - p * h * 0.25f
                val y = -len + p * (h + len * 2)
                paint.strokeWidth = u * (if (far) 1f else 1.5f); paint.alpha = if (far) 38 else 70
                canvas.drawLine(x, y, x - len * 0.25f, y + len, paint)
            }
        }
    }

    /** Dust specks blown along the ground by the wind. */
    class Dust(ctx: Context) : EffectView(ctx) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE8B48A.toInt() }
        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            if (w <= 0f) return
            val u = w / 1920f; val t = now()
            for (i in 0 until 90) {
                val sp = u * (110f + 190f * hash(i, 1))
                val span = w + 80f * u
                val x = (hash(i, 2) * span + sp * t) % span - 40f * u
                val y = h * (0.35f + 0.6f * hash(i, 3)) + sin((t * (0.8f + hash(i, 4)) + hash(i, 5) * 6.28f).toDouble()).toFloat() * u * 14f
                paint.alpha = (60 + 130 * hash(i, 6)).toInt()
                canvas.drawCircle(x, y, u * (1.2f + 2.4f * hash(i, 7)), paint)
            }
        }
    }

    /** The Earth over the lunar horizon: a few pale cloud wisps sliding very slowly across its disc, and nothing else moves. */
    class Earth(ctx: Context) : EffectView(ctx) {
        private val glow = glowBitmap()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN) }
        private val dst = RectF()
        private val clip = Path()
        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            if (w <= 0f) return
            val c = Crop(w, h); val t = now()
            val cx = c.x(0.8036f); val cy = c.y(0.2147f); val r = 0.01645f * IMG_W * c.s
            clip.reset(); clip.addCircle(cx, cy, r * 0.97f, Path.Direction.CW)
            canvas.save(); canvas.clipPath(clip)
            for (i in 0 until 4) {
                val x = cx - r * 1.6f + ((t * 0.7f * (0.8f + 0.4f * hash(i, 1)) + hash(i, 2) * r * 3.2f) % (r * 3.2f))
                val y = cy + (hash(i, 3) - 0.5f) * r * 1.4f
                paint.alpha = 70
                dst.set(x - r * 0.7f, y - r * 0.16f, x + r * 0.7f, y + r * 0.16f)
                canvas.drawBitmap(glow, null, dst, paint)
            }
            canvas.restore()
        }
    }

    /** Scattered glint spots for a band of the picture, from the same fixed pattern every time. */
    private fun ground(n: Int, y0: Float, y1: Float) = FloatArray(n * 3) { k -> val i = k / 3; when (k % 3) { 0 -> 0.03f + 0.94f * hash(i, 21); 1 -> y0 + (y1 - y0) * hash(i, 22); else -> 0.25f + 0.2f * hash(i, 23) } }

    /** Stadium floodlights: each cluster breathes in turn, a wave running down the roof twice every 8 seconds, and its light beam swings with it. */
    class Floodlights(ctx: Context, private val pts: FloatArray) : EffectView(ctx) {
        private val glow = glowBitmap()
        private val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = PorterDuffColorFilter(0xFFD2E8FF.toInt(), PorterDuff.Mode.SRC_IN); xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD) }
        private val beam = Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = PorterDuffColorFilter(0xFFB8D6FF.toInt(), PorterDuff.Mode.SRC_IN); xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD) }
        private val dst = RectF()
        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            if (w <= 0f) return
            val c = Crop(w, h); val u = w / 1920f
            val loop = (SystemClock.uptimeMillis() % 8000L) / 8000f * 6.2832f      // one seamless 8 second round
            for (j in 0 until pts.size / 2) {
                val x = c.x(pts[j * 2]); val y = c.y(pts[j * 2 + 1])
                val ph = -j * 3.1416f / 2.2f
                val a = (0.5f + 0.5f * sin(2f * loop + ph)).pow(1.4f)
                val r = u * 105f
                halo.alpha = (255 * (0.20f + 0.75f * a)).toInt().coerceIn(0, 255)
                dst.set(x - r, y - r, x + r, y + r); canvas.drawBitmap(glow, null, dst, halo)
                canvas.save(); canvas.translate(x, y); canvas.rotate(32f + 9f * sin(loop + ph))
                beam.alpha = (255 * (0.10f + 0.50f * a)).toInt().coerceIn(0, 255)
                dst.set(u * 10f, -u * 26f, u * 360f, u * 26f); canvas.drawBitmap(glow, null, dst, beam)
                canvas.restore()
            }
        }
    }

    /** A small cross of light that flares on the brightest stars, each on its own beat. */
    class Sparks(ctx: Context, private val pts: FloatArray) : EffectView(ctx) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE6F0FF.toInt(); strokeCap = Paint.Cap.ROUND }
        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            if (w <= 0f) return
            val c = Crop(w, h); val u = w / 1920f; val t = now()
            for (i in 0 until pts.size / 2) {
                val a = (0.5f + 0.5f * sin(t * 6.2832f / (4f + 4f * hash(i, 5)) + hash(i, 6) * 6.2832f)).pow(3f)
                if (a < 0.04f) continue
                val x = c.x(pts[i * 2]); val y = c.y(pts[i * 2 + 1]); val l = u * (7f + 9f * a)
                paint.alpha = (200 * a).toInt().coerceIn(0, 255); paint.strokeWidth = u * 1.3f
                canvas.drawLine(x - l, y, x + l, y, paint); canvas.drawLine(x, y - l, x, y + l, paint)
            }
        }
    }

    val all: List<Theme> = listOf(
        Theme("rockies", "Snowy Rockies", R.drawable.tv_wp_rockies, 120, 190, null) { c -> listOf(Snow(c)) },
        Theme("christmas", "Christmas Forest", R.drawable.tv_wp_christmas, 70, 140, null) { c -> listOf(Sparkles(c, BULBS, 0xFFFFC066.toInt(), 3, 0.22f, 0.95f, 15f, 6f), Snow(c, 0.6f)) },
        Theme("autumn", "Autumn Harvest", R.drawable.tv_wp_autumn, 110, 185, Warp(R.drawable.tv_wm_autumn, 1, 7f, 0f)) { c -> listOf(FallLeaves(c, 7)) },
        Theme("coast", "Summer Coast", R.drawable.tv_wp_coast, 110, 185, Warp(R.drawable.tv_wm_coast, 0, 4f, 0f)) { _ -> emptyList() },
        Theme("farm", "Country Farmhouse", R.drawable.tv_wp_farm, 110, 185, Warp(R.drawable.tv_wm_farm, 1, 8f, 0f)) { c -> listOf(Motes(c, 0xFFFFE8A8.toInt(), 26)) },
        Theme("lake", "Moonlit Lake", R.drawable.tv_wp_lake, 60, 130, Warp(R.drawable.tv_wm_lake, 0, 3.5f, 0f)) { c -> listOf(Haze(c, 4), Rain(c)) },
        Theme("milkyway", "Milky Way", R.drawable.tv_wp_milkyway, 20, 70, null) { c -> listOf(Sparkles(c, MILKYWAY, 0xFFEAF0FF.toInt(), 1, 0.35f, 1f, 9f, 4.5f), Meteor(c)) },
        Theme("seattle", "Seattle", R.drawable.tv_wp_seattle, 110, 185, Warp(R.drawable.tv_wm_seattle, 0, 3f, 0f)) { _ -> emptyList() },
        Theme("ozarks", "The Ozarks", R.drawable.tv_wp_ozarks, 110, 185, Warp(R.drawable.tv_wm_ozarks, 0, 3.5f, 0f)) { _ -> emptyList() },
        Theme("nyc", "New York City", R.drawable.tv_wp_nyc, 110, 185, Warp(R.drawable.tv_wm_nyc, 0, 3f, 0f)) { _ -> emptyList() },
        Theme("niagara", "Waterfall", R.drawable.tv_wp_niagara, 110, 185, null) { c -> listOf(Haze(c, 1)) },
        Theme("bora", "Tropical Lagoon", R.drawable.tv_wp_bora, 105, 180, Warp(R.drawable.tv_wm_bora, 0, 3.5f, 0f)) { _ -> emptyList() },
        Theme("meadow", "Spring Meadow", R.drawable.tv_wp_meadow, 110, 185, Warp(R.drawable.tv_wm_meadow, 1, 8f, 0f)) { c -> listOf(Motes(c, 0xFFFFFFFF.toInt(), 30), Butterflies(c)) },
        Theme("starry", "Starry Countryside", R.drawable.tv_wp_starry, 50, 110, null) { c -> listOf(Sparkles(c, STARRY, 0xFFFFFFFF.toInt(), 1, 0.2f, 1f, 10f, 3.5f), Meteor(c)) },
        Theme("mars", "Mars", R.drawable.tv_wp_mars, 70, 150, null) { c -> listOf(Haze(c, 0), Dust(c)) },
        Theme("moon", "The Moon", R.drawable.tv_wp_moon, 60, 130, null) { c -> listOf(Earth(c)) },
        Theme("pinksnow", "Snowy Pink Sky", R.drawable.tv_wp_pinksnow, 60, 150, null) { c -> listOf(Sparkles(c, ground(40, 0.66f, 0.98f), 0xFFFFFFFF.toInt(), 1, 0.15f, 0.9f, 7f, 3f), Snow(c, 0.5f)) },
        Theme("stadium", "Seattle Stadium", R.drawable.tv_wp_stadium, 70, 150, null) { c -> listOf(Sparkles(c, STADIUM_STARS, 0xFFFFFFFF.toInt(), 1, 0.12f, 1f, 6f, 4f), Sparks(c, STADIUM_SPARKS), Sparkles(c, STADIUM_RIM, 0xFFFFE9C7.toInt(), 1, 0.15f, 1f, 5f, 3f), Floodlights(c, STADIUM_LAMPS), Meteor(c, 9f)) },
        Theme("clouds", "Above the Clouds", R.drawable.tv_wp_clouds, 120, 195, null) { c -> listOf(Haze(c, 3)) })

    fun byId(id: String): Theme? = all.firstOrNull { it.id == id }

    private fun decode(ctx: Context, res: Int): Bitmap {
        val o = BitmapFactory.Options().apply { inScaled = false }
        return BitmapFactory.decodeResource(ctx.resources, res, o)
    }

    /** The layers for one theme, bottom to top: the picture, a dark wash so white text stays readable, then the moving parts. */
    fun build(ctx: Context, t: Theme): List<View> {
        val out = ArrayList<View>()
        val video = Videos.file(ctx, t.id)
        if (video != null) {      // a downloaded video replaces the moving effects; the still picture stays underneath until it plays
            out.add(ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_CROP; setImageResource(t.img) })
            out.add(Videos.LoopVideo(ctx, video))
            out.add(View(ctx).apply { background = android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf((t.top shl 24) or 0x08090B, (t.bottom shl 24) or 0x08090B)) })
            out.forEach { it.isFocusable = false; it.isClickable = false; it.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
            return out
        }
        val warp = t.warp
        if (warp != null && warp.kind == 0) out.add(WallpaperView(ctx, decode(ctx, t.img), decode(ctx, warp.mask), 0, warp.ground, 0f))
        else out.add(ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_CROP; setImageResource(t.img) })
        if (warp != null) out.add(Shimmer(ctx, decode(ctx, warp.mask), warp.kind, if (warp.kind == 1) 22 else if (warp.kind == 2) 60 else 40))
        out.add(View(ctx).apply { background = android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf((t.top shl 24) or 0x08090B, (t.bottom shl 24) or 0x08090B)) })
        out.addAll(t.effects(ctx))
        out.forEach { it.isFocusable = false; it.isClickable = false; it.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
        return out
    }
}
