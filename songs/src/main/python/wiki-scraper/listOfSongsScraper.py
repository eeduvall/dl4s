import urllib3
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)


import requests
import re
from bs4 import BeautifulSoup
import json

def is_visible(element):
    if element.parent is None:
        return True

    if element.get("style") and "display: none" in element.get("style"):
        return False

    return is_visible(element.parent)

def find_songs_in_link(relative_link, file):
    # Send a GET request to the starting URL
    print('link is: ', starting_url + relative_link)
    response = requests.get(starting_url + relative_link, verify=False)

    # Parse the HTML content of the response
    soup = BeautifulSoup(response.content, "html.parser")

    # Find all the links on the page
    links = soup.find_all("a")

    find_song_info(links, file)

    # Find the link with text that starts as "Next page (List of songs recorded by"
    next_page_link = None
    for link in links:
        if link.text.startswith("Next page (List of songs recorded by"):
            next_page_link = link.get("href")
            break

    # Check if a next page link was found
    if next_page_link:
        # Call the function recursively with the next page link
        find_songs_in_link(next_page_link, file)

def find_song_info(links, file):
    # Iterate over the links
    for link in links:
        href = link.get("href")
        title = link.get("title")
        if title and title.startswith('List of songs recorded by'):
            # Extract the artist from the title
            artist = title.replace("List of songs recorded by ", "")

            # Construct the absolute URL of the linked page
            absolute_url = starting_url + href

            # Send a GET request to the linked page
            linked_response = requests.get(absolute_url, verify=False)

            # Parse the HTML content of the linked response
            linked_soup = BeautifulSoup(linked_response.content, "html.parser")
            # print(linked_soup.prettify())

            # Do something with the linked page, such as extracting data or navigating further
            # ...

            # Print the title of the linked page as an example
            print(linked_soup.title.text)
            
            # Loop over all table elements
            has_song_table = False
            song_tables = []
            for table in linked_soup.find_all("table"):
                # Check if there is a header "Song"
                # print('th values are: ', table.find("th"))
                # Check if the table is visible
                if not is_visible(table):
                    continue
                has_song_header = False
                has_album_header = False
                has_year_header = False
                for th in table.find_all("th"):
                    if "song" in th.text.lower() or "title" in th.text.lower():
                        has_song_header = True
                    if "album" in th.text.lower() or 'release' in th.text.lower():
                        has_album_header = True
                    if "year" in th.text.lower():
                        has_year_header = True

                if has_song_header and has_album_header:
                    has_song_table = True
                    song_tables.append(table)

            # valid_songs = []
            for table in song_tables:
                song_column = -1
                album_column = -1
                year_column = -1
                column = -1
                for th in table.find_all("th"):
                    column = column + 1
                    if "song" in th.text.lower() or "title" in th.text.lower():
                        # print('song column is ', column)
                        song_column = column
                    if "album" in th.text.lower() or 'release' in th.text.lower():
                        # print('album column is ', column)
                        album_column = column
                    if "year" in th.text.lower():
                        # print('year column is ', column)
                        year_column = column
                for tr in table.find_all('tr'):
                    # print('tr is: ', tr)
                    tds = tr.find_all('td')
                    if len(tds) == 0 or len(tds) < (song_column+1) or len(tds) < (album_column+1):
                        continue
                    # print('tds are: ', tds)
                    song_obj = {
                        'song': tds[song_column].text.rstrip('\n') if tds[song_column].text.endswith('\n') else tds[song_column].text,
                        'album': tds[album_column].text.rstrip('\n') if tds[album_column].text.endswith('\n') else tds[album_column].text
                    }
                    if (year_column != -1 and len(tds) > year_column):
                        song_obj['year'] = tds[year_column].text.rstrip('\n') if tds[year_column].text.endswith('\n') else tds[year_column].text
                    if (len(song_obj['song']) < 2
                        or len(song_obj['album']) < 2
                        or "\n" in song_obj['song']
                        or "\n" in song_obj['album']
                        or ('year' in song_obj.keys()
                            and (len(song_obj['year']) < 4
                                or not bool(re.match('^[0-9]+$', song_obj['year']))))):
                        continue
                    if ('year' in song_obj.keys()):
                        file.write(json.dumps({"artist": artist, "album": song_obj['album'], "song": song_obj['song'], "year": song_obj['year']}) + ",\n")
                    else:
                        file.write(json.dumps({"artist": artist, "album": song_obj['album'], "song": song_obj['song']}) + ",\n")

#Run the program
                        
# Define the path and filename for the JSON file
json_file_path = "/Users/duvalle/Documents/GitHub/dl4s/songs/src/main/python/wiki-scraper/songs.json"
file = open(json_file_path, "w")
file.write('[')

# Define the URL of the starting page
starting_url = "https://en.wikipedia.org"

find_songs_in_link("/wiki/Special:PrefixIndex/List_of_songs_recorded_by", file)

file.write(']')
file.close()