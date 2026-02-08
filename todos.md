review new architure and docs/ and specially the article schema
look at resource syncerrs rsssycn,ytsync etc and see if you can reuse code
see if you can reuse code in resource viewers and have consistant ui/ux but still use unique features of each source

review the settigns page and data persistance there, make dir for and and spit it , remove dummy cposcomposes in settings.

make navigation survive process kill so if user was reading an article and came back it's still on that article

Add per-source sync results (including lastError per source) and update UI surfaces to show partial failures


add support for images in rss,medium?
add support for open graph images
add support for favicon
better unrea styles, better item style, 
shoud save be in the outside too?
better toggable summary and visually distinct
in the sources filter sheet show the unread messages count for each source.
add the ability to pin resources by having a icon next to it
add brand colors (tints) per source type for chips and menu items.
---
add hackmd,hackernews, devio, substack, notion pages, reddit, obsidian publish, discourse, github releases,github discussions, github issues, 
---
add pubmed, arvix/
import and export ompol for rss sources
import and export and backup for this app(apps own representation of data)

paging
limit source items(in settings default 100) and purge old
make our own podcast website the default for rss. what to do when we change rss url and the user didn't update app
make manage sources only appear in feed and make it an icon?
make add source auto detectable when possible, show sources as a list with icons instead or horziontally scrolling. 
make plugin registery from manual in appdepedencies to using koin


for a random blog can you take the linke and detect if it has RSS field tags in it? can you figure out