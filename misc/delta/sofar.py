#!/usr/bin/env python
import os

if __name__ == '__main__':
    def process_line(line):
        items = line.split(' ')
        delim = [a for a in items if ':' in a][0]
        parts = line.split(delim)
        return parts[1].strip()


    ##

    sofar_files = [a for a in os.listdir('/Volumes/seagate/done') if not a.startswith('._')]
    sofar_files = [f.replace('.zip', '') for f in sofar_files]
    sofar_files.sort()

    ##
    index_files = [f.strip() for f in open('/Volumes/seagate/index.txt', 'r').readlines()]
    index_files.sort()

    print(sofar_files)
    print(index_files)

    print(set(sofar_files) - set(index_files))
    print(set(index_files) - set(sofar_files))
