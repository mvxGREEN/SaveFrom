import yt_dlp
import re
import random
from com.chaquo.python import Python
from com.mvxgreen.ytdloader import MainActivity

def download_video(activity, video_url, out):
    # 'outtmpl': out + '%(title).25s.%(ext)s',
    progress_hook = create_progress_hook(activity)
    # prevent overwrite with random id
    filename_id = f"{random.randint(1,10)}{random.randint(1,10)}{random.randint(1,10)}{random.randint(1,10)}_"

    ydl_opts = {
        #'format': "bestvideo",
        'outtmpl': out + filename_id + '%(title).25s.mp4',
        'restrictfilenames': True,
        'progress_hooks': [progress_hook]
    }
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info_dict = ydl.extract_info(video_url, download=True)
        filename = info_dict['title'][0:25]
        return filename_id + sanitize_filename(filename)

def create_progress_hook(a):
    def progress_hook(d):
        if d['status'] == 'downloading':
            MainActivity.setProgress(a, d['_percent_str'])
        if d['status'] == 'finished':
            MainActivity.setProgress(a, d['_percent_str'])
    return progress_hook


def extract_video_title(video_url):
    ydl_opts = {
        #'format': "bestvideo",
        'restrictfilenames': True,
    }
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info_dict = ydl.extract_info(video_url, download=False)

        return info_dict['title'][0:25]

def extract_video_ext(video_url):
    ydl_opts = {
        #'format': "bestvideo",
    }
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info_dict = ydl.extract_info(video_url, download=False)
        return info_dict['ext']

def extract_video_dl_url(video_url):
    ydl_opts = {
        #'format': "bestvideo",
    }
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info_dict = ydl.extract_info(video_url, download=False)
        return info_dict['url']

def extract_video_thumbnail(video_url):
    ydl_opts = {
        #'format': "bestvideo",
    }
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info_dict = ydl.extract_info(video_url, download=False)
        return info_dict['thumbnail']

def sanitize_filename(filename):
    """Removes or replaces sensitive characters from a filename.

    Args:
        filename (str): The filename to sanitize.

    Returns:
        str: The sanitized filename.
    """

    # 1. Remove or replace characters that are invalid across platforms
    filename = re.sub(r'[<>:"/\\|?*\x00-\x1F]', '_', filename)

    # 2. Remove or replace characters that might cause issues with specific OS
    filename = filename.replace(' ', '_') # replace spaces
    filename = filename.strip('. ')  # Remove leading/trailing spaces and dots

    # 3. Remove potentially problematic characters
    filename = re.sub(r'[,;!@#\$%^&()+]', '', filename)

    # 4. Normalize Unicode characters
    filename = filename.encode('ascii', 'ignore').decode('ascii')

    return filename