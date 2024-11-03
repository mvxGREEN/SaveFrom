import yt_dlp

def download_video(video_url, out):
    # 'outtmpl': out + '%(title)s.%(ext)s',
    ydl_opts = {
        'format': "best[ext=mp4][height<=?1080]",
        'outtmpl': out + '%(title).25s.%(ext)s'
    }
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info_dict = ydl.extract_info(video_url, download=True)
        return info_dict['title'][0:25]

def extract_video_title(video_url):
    ydl_opts = {
        'format': "best[ext=mp4][height<=?1080]",
    }
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info_dict = ydl.extract_info(video_url, download=False)
        return info_dict['title']

def extract_video_ext(video_url):
    ydl_opts = {
        'format': "best[ext=mp4][height<=?1080]",
    }
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info_dict = ydl.extract_info(video_url, download=False)
        return info_dict['ext']

def extract_video_dl_url(video_url):
    ydl_opts = {
        'format': "best[ext=mp4][height<=?1080]",
    }
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info_dict = ydl.extract_info(video_url, download=False)
        return info_dict['url']

def extract_video_thumbnail(video_url):
    ydl_opts = {
        'format': "best[ext=mp4][height<=?1080]",
    }
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info_dict = ydl.extract_info(video_url, download=False)
        return info_dict['thumbnail']