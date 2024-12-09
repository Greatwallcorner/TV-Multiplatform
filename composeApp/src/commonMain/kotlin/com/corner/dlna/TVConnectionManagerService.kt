package com.corner.dlna

import org.jupnp.support.connectionmanager.ConnectionManagerService
import org.jupnp.support.model.Protocol
import org.jupnp.support.model.ProtocolInfo
import org.jupnp.support.model.ProtocolInfos
import org.jupnp.util.MimeType


class TVConnectionManagerService: ConnectionManagerService() {
    override fun getSourceProtocolInfo(): ProtocolInfos {
        return ProtocolInfos( //this one overlap all ???
            ProtocolInfo(
                Protocol.HTTP_GET,
                ProtocolInfo.WILDCARD,
                MimeType.WILDCARD,
                ProtocolInfo.WILDCARD
            ),  //this one overlap all images ???
            ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "image/" + MimeType.WILDCARD, ProtocolInfo.WILDCARD),
            ProtocolInfo(
                Protocol.HTTP_GET,
                ProtocolInfo.WILDCARD,
                "audio",
                ProtocolInfo.WILDCARD
            ),  //this one overlap all audio ???
            ProtocolInfo(
                Protocol.HTTP_GET,
                ProtocolInfo.WILDCARD,
                "audio/" + MimeType.WILDCARD,
                ProtocolInfo.WILDCARD
            ),  //this one overlap all video ???
            ProtocolInfo(
                Protocol.HTTP_GET,
                ProtocolInfo.WILDCARD,
                "video/" + MimeType.WILDCARD,
                ProtocolInfo.WILDCARD
            ),  //IMAGE
            ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "image/jpeg", "DLNA.ORG_PN=JPEG_TN"),
            ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "image/jpeg", "DLNA.ORG_PN=JPEG_SM"),
            ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "image/jpeg", "DLNA.ORG_PN=JPEG_MED"),
            ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "image/jpeg", "DLNA.ORG_PN=JPEG_LRG"),
            ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "image/jpeg", "DLNA.ORG_PN=JPEG_RES_H_V"),
            ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "image/png", "DLNA.ORG_PN=PNG_TN"),
            ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "image/png", "DLNA.ORG_PN=PNG_LRG"),
            ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "image/png", "DLNA.ORG_PN=PNG_LRG"),
            ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "image/png", "DLNA.ORG_PN=PNG_LRG"),
            ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "image/gif", "DLNA.ORG_PN=GIF_LRG"),  //AUDIO
            ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "audio/mpeg", "DLNA.ORG_PN=MP3"),
            ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "audio/L16", "DLNA.ORG_PN=LPCM"),  //VIDEO
            ProtocolInfo(
                Protocol.HTTP_GET,
                ProtocolInfo.WILDCARD,
                "video/mpeg",
                "DLNA.ORG_PN=AVC_TS_HD_24_AC3_ISO;SONY.COM_PN=AVC_TS_HD_24_AC3_ISO"
            ),
            ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "video/mpeg", "DLNA.ORG_PN=MPEG_TS_SD_EU_ISO"),
            ProtocolInfo(
                Protocol.HTTP_GET,
                ProtocolInfo.WILDCARD,
                "video/mpeg",
                "DLNA.ORG_PN=MPEG_TS_HD_50_L2_ISO;SONY.COM_PN=HD2_50_ISO"
            ),
            ProtocolInfo(
                Protocol.HTTP_GET,
                ProtocolInfo.WILDCARD,
                "video/mpeg",
                "DLNA.ORG_PN=MPEG_TS_HD_60_L2_ISO;SONY.COM_PN=HD2_60_ISO"
            ),
            ProtocolInfo(
                Protocol.HTTP_GET,
                ProtocolInfo.WILDCARD,
                "video/vnd.dlna.mpeg-tts",
                "DLNA.ORG_PN=AVC_TS_HD_24_AC3;SONY.COM_PN=AVC_TS_HD_24_AC3"
            ),
            ProtocolInfo(
                Protocol.HTTP_GET,
                ProtocolInfo.WILDCARD,
                "video/vnd.dlna.mpeg-tts",
                "DLNA.ORG_PN=AVC_TS_HD_24_AC3_T;SONY.COM_PN=AVC_TS_HD_24_AC3_T"
            ),
            ProtocolInfo(
                Protocol.HTTP_GET,
                ProtocolInfo.WILDCARD,
                "video/vnd.dlna.mpeg-tts",
                "DLNA.ORG_PN=MPEG_PS_PAL"
            ),
            ProtocolInfo(
                Protocol.HTTP_GET,
                ProtocolInfo.WILDCARD,
                "video/vnd.dlna.mpeg-tts",
                "DLNA.ORG_PN=MPEG_PS_NTSC"
            ),
            ProtocolInfo(
                Protocol.HTTP_GET,
                ProtocolInfo.WILDCARD,
                "video/vnd.dlna.mpeg-tts",
                "DLNA.ORG_PN=MPEG_TS_SD_50_L2_T"
            ),
            ProtocolInfo(
                Protocol.HTTP_GET,
                ProtocolInfo.WILDCARD,
                "video/vnd.dlna.mpeg-tts",
                "DLNA.ORG_PN=MPEG_TS_SD_50_AC3_T"
            ),
            ProtocolInfo(
                Protocol.HTTP_GET,
                ProtocolInfo.WILDCARD,
                "video/vnd.dlna.mpeg-tts",
                "DLNA.ORG_PN=MPEG_TS_SD_60_L2_T"
            ),
            ProtocolInfo(
                Protocol.HTTP_GET,
                ProtocolInfo.WILDCARD,
                "video/vnd.dlna.mpeg-tts",
                "DLNA.ORG_PN=MPEG_TS_SD_60_AC3_T"
            ),
            ProtocolInfo(
                Protocol.HTTP_GET,
                ProtocolInfo.WILDCARD,
                "video/vnd.dlna.mpeg-tts",
                "DLNA.ORG_PN=MPEG_TS_SD_EU"
            ),
            ProtocolInfo(
                Protocol.HTTP_GET,
                ProtocolInfo.WILDCARD,
                "video/vnd.dlna.mpeg-tts",
                "DLNA.ORG_PN=MPEG_TS_SD_EU_T"
            ),
            ProtocolInfo(
                Protocol.HTTP_GET,
                ProtocolInfo.WILDCARD,
                "video/vnd.dlna.mpeg-tts",
                "DLNA.ORG_PN=MPEG_TS_HD_50_L2_T;SONY.COM_PN=HD2_50_T"
            ),
            ProtocolInfo(
                Protocol.HTTP_GET,
                ProtocolInfo.WILDCARD,
                "video/vnd.dlna.mpeg-tts",
                "DLNA.ORG_PN=MPEG_TS_HD_60_L2_T;SONY.COM_PN=HD2_60_T"
            ),
            ProtocolInfo(
                Protocol.HTTP_GET,
                ProtocolInfo.WILDCARD,
                "video/vnd.dlna.mpeg-tts",
                "DLNA.ORG_PN=AVC_TS_HD_50_AC3;SONY.COM_PN=AVC_TS_HD_50_AC3"
            ),
            ProtocolInfo(
                Protocol.HTTP_GET,
                ProtocolInfo.WILDCARD,
                "video/vnd.dlna.mpeg-tts",
                "DLNA.ORG_PN=AVC_TS_HD_60_AC3;SONY.COM_PN=AVC_TS_HD_60_AC3"
            ),
            ProtocolInfo(
                Protocol.HTTP_GET,
                ProtocolInfo.WILDCARD,
                "video/vnd.dlna.mpeg-tts",
                "DLNA.ORG_PN=AVC_TS_HD_50_AC3_T;SONY.COM_PN=AVC_TS_HD_50_AC3_T"
            ),
            ProtocolInfo(
                Protocol.HTTP_GET,
                ProtocolInfo.WILDCARD,
                "video/vnd.dlna.mpeg-tts",
                "DLNA.ORG_PN=AVC_TS_HD_60_AC3_T;SONY.COM_PN=AVC_TS_HD_60_AC3_T"
            ),
            ProtocolInfo(Protocol.HTTP_GET, ProtocolInfo.WILDCARD, "video/x-mp2t-mphl-188", ProtocolInfo.WILDCARD)
        )
    }
}