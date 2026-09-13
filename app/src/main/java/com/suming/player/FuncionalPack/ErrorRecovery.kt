package com.suming.player.FuncionalPack

import androidx.media3.common.PlaybackException

object ErrorRecovery {

    const val ERROR_SOURCE_ERROR = "Source error"
    const val ERROR_SOURCE_ERROR_C_INVALID_NAL_LENGTH = "Invalid NAL length"

    const val ERROR_SOURCE_UNEXPECTED_RUNTIME_ERROR = "Unexpected runtime error"

    const val ECN_ERROR_CODE_DECODER_INIT_FAILED = "ERROR_CODE_DECODER_INIT_FAILED"



    //返回值1：是否清除媒体  返回值2：是否重新上线  返回3：调用其他
    fun recover(error: PlaybackException): Triple<Boolean, Boolean, Boolean>{
        when(error.message){
            ERROR_SOURCE_ERROR -> {
                val cause = error.cause.toString()

                when{
                    cause.contains(ERROR_SOURCE_ERROR_C_INVALID_NAL_LENGTH) -> {
                        return Triple(true, false, true)
                    }

                    else -> {
                        return Triple(true, false, false)
                    }
                }
            }
            ERROR_SOURCE_UNEXPECTED_RUNTIME_ERROR -> {


                return Triple(false, true, false)
            }
            else -> {
                val ECN = error.errorCodeName
                when(ECN){
                    ECN_ERROR_CODE_DECODER_INIT_FAILED -> {
                        return Triple(true, false, false)
                    }


                    else -> {
                        return Triple(true, false, false)
                    }
                }

            }

        }
    }



    //典型错误留档
    /*
        ERROR 1
        MESSAGE:androidx.media3.exoplayer.ExoPlaybackException: Source error,
        MESSAGE:Source error,
        CAUSE:androidx.media3.common.ParserException: Invalid NAL length {contentIsMalformed=true, dataType=1},
        ECN:ERROR_CODE_PARSING_CONTAINER_MALFORMED

        ERROR 2
        ERROR:androidx.media3.exoplayer.ExoPlaybackException: Unexpected runtime error,
        MESSAGE:Unexpected runtime error,
        CAUSE:androidx.media3.exoplayer.ExoTimeoutException: Detaching surface timed out.,
        ECN:ERROR_CODE_TIMEOUT

        ERROR 3
        ERROR:androidx.media3.exoplayer.ExoPlaybackException: MediaCodecVideoRenderer error, index=0, format=Format(1, null, video/mp4, video/avc, avc1.640028, 226298, und, [1280, 674, 25.0, ColorInfo(BT709, Limited range, SDR SMPTE 170M, false, 8bit Luma, 8bit Chroma)], [-1, -1, -1]), format_supported=YES,
        MESSAGE:MediaCodecVideoRenderer error, index=0, format=Format(1, null, video/mp4, video/avc, avc1.640028, 226298, und, [1280, 674, 25.0, ColorInfo(BT709, Limited range, SDR SMPTE 170M, false, 8bit Luma, 8bit Chroma)], [-1, -1, -1]), format_supported=YES,
        CAUSE:androidx.media3.exoplayer.mediacodec.MediaCodecRenderer$DecoderInitializationException: Decoder init failed: OMX.IMG.MSVDX.Decoder.AVC, Format(1, null, video/mp4, video/avc, avc1.640028, 226298, und, [1280, 674, 25.0, ColorInfo(BT709, Limited range, SDR SMPTE 170M, false, 8bit Luma, 8bit Chroma)], [-1, -1, -1]),
        ECN:ERROR_CODE_DECODER_INIT_FAILED





     */

}