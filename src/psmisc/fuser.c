/* loosly based on fuser.c by Werner Almesberger */

/* Copyright 1993-1998 Werner Almesberger. See file COPYING for details. 
psmisc (fuser, killall and pstree) program code, documentation and
auxiliary programs are
Copyright 1993-1998 Werner Almesberger.
All rights reserved.

Redistribution and use in source and binary forms of parts of or the
whole original or derived work are permitted provided that the
original work is properly attributed to the author. The name of the
author may not be used to endorse or promote products derived from
this software without specific prior written permission. This work
is provided "as is" and without any express or implied warranties.
*/


#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <ctype.h>
#include <unistd.h>
#include <dirent.h>
#include <pwd.h>
#include <signal.h>
#include <limits.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <linux/kdev_t.h>
#include <linux/major.h>
#define COMM_LEN 16
#define PROC_BASE  "/proc"
#define UID_UNKNOWN -1
#define NAME_FIELD 20 /* space reserved for file name */

#define MAX_LINE 256 /* longest line we may ever find in /proc */


#ifndef LOOP_MAJOR /* don't count on the headers too much ... */
#define LOOP_MAJOR 7
#endif


#define REF_FILE   1	/* an open file */
#define REF_ROOT   2	/* current root */
#define REF_CWD    4	/* current directory */
#define REF_EXE    8	/* executable */
#define REF_MMAP  16	/* mmap'ed file or library */

#define FLAG_KILL  1	/* kill process */
#define FLAG_UID   2	/* show uid */
#define FLAG_VERB  4	/* show verbose output */
#define FLAG_DEV   8	/* show all processes using this device */
#define FLAG_ASK  16	/* ask before killing a process */


typedef struct _unix_cache {
    dev_t fs_dev;
    ino_t fs_ino;
    ino_t net_ino;
    struct _unix_cache *next;
} UNIX_CACHE;

typedef struct {
    const char *name;
    int once;
} SPACE_DSC;

typedef enum { it_proc,it_mount,it_loop,it_swap } ITEM_TYPE;

typedef struct item_dsc {
    ITEM_TYPE type;
    union {
	struct {
	    pid_t pid;
	    int uid; /* must also accept UID_UNKNOWN */
	    int ref_set;
	} proc;
	struct {
	    const char *path;
	} misc;
    } u;
    struct item_dsc *next;
} ITEM_DSC;

typedef struct file_dsc {
    const char *name;  /* NULL if previous entry has name */
    dev_t dev;
    ino_t ino;
    int flags,sig_num;
    SPACE_DSC *name_space; /* or NULL if no indication */
    ITEM_DSC *items;
    struct file_dsc *named,*next;
} FILE_DSC;

static SPACE_DSC name_spaces[] = {
    { "file",  0 }, /* must be first */
    { "tcp",  0 },
    { "udp",  0 },
    { NULL,   0 }
};


static FILE_DSC *files = NULL;
static FILE_DSC *last_named = NULL;
static UNIX_CACHE *unix_cache = NULL;
static int all = 0,found_item = 0;

static void add_file(const char *path,unsigned long device,unsigned long inode, pid_t pid,int ref)
{
    struct stat st;
    FILE_DSC *file,*next;
    ITEM_DSC **item,*this;
    unsigned long mount_dev;

    if (device) mount_dev = device;
    for (file = files; file; file = next) {
	next = file->next;
	if (file->flags & FLAG_DEV ? mount_dev && mount_dev == file->dev :
	  device == file->dev && inode == file->ino) {
	    if (!file->name) file = file->named;
	    for (item = &file->items; *item; item = &(*item)->next)
		if ((*item)->type == it_proc && (*item)->u.proc.pid >= pid)
		    break;
	    if (*item && (*item)->u.proc.pid == pid) this = *item;
	    else {
		if (!(this = malloc(sizeof(ITEM_DSC)))) {
		    perror("malloc");
		    exit(1);
		}
		this->type = it_proc;
		this->u.proc.pid = pid;
		this->u.proc.uid = UID_UNKNOWN;
		this->u.proc.ref_set = 0;
		this->next = *item;
		*item = this;
		found_item = 1;
	    }
	    this->u.proc.ref_set |= ref;
	    if ((file->flags & (FLAG_UID | FLAG_VERB)) && this->u.proc.uid == UID_UNKNOWN && lstat(path,&st) >= 0) 
		this->u.proc.uid = st.st_uid;
	}
    }
}
static void check_link(const char *path,pid_t pid,int type)
{
    struct stat st;

    if (stat(path,&st) >= 0)
	add_file(path,st.st_dev,st.st_ino,pid,type);
}

static void check_dir(const char *rel,pid_t pid,int type)
{
    DIR *dir;
    struct dirent *de;
    char path[PATH_MAX+1];

    if (!(dir = opendir(rel))) return;
    while (de = readdir(dir))
	if (strcmp(de->d_name,".") && strcmp(de->d_name,"..")) {
	    sprintf(path,"%s/%s",rel,de->d_name);
	    check_link(path,pid,type);
	}
    (void) closedir(dir);
}


extern void scan_fd(void)
{
    DIR *dir;
    struct dirent *de;
    char path[PATH_MAX+1];
    pid_t pid;
    int empty;

    if (!(dir = opendir(PROC_BASE))) {
	perror(PROC_BASE);
	exit(1);
    }
    empty = 1;
    while (de = readdir(dir))
	if (pid = atoi(de->d_name)) {
	    empty = 0;
	    sprintf(path,"%s/%d",PROC_BASE,pid);
	    if (chdir(path) >= 0) {
		check_link("root",pid,REF_ROOT);
		check_link("cwd",pid,REF_CWD);
		check_link("exe",pid,REF_EXE);
		check_dir("lib",pid,REF_MMAP);
		check_dir("mmap",pid,REF_MMAP);
		check_dir("fd",pid,REF_FILE);
	    }
	}
    (void) closedir(dir);
    if (empty) {
	fprintf(stderr,PROC_BASE " is empty (not mounted ?)\n");
	exit(1);
    }
}

extern void show_user(char tstring[])
{
    const ITEM_DSC *item;
    FILE *f;
    const struct passwd *pw;
    const char *user,*scan;
    char tmp[10],path[PATH_MAX+1],comm[COMM_LEN+1];
    int length,dummy;
    pid_t self;
    const char *name;
    int uid;

    parse_args(tstring);
    scan_fd();
    if (seteuid(getuid()) < 0) {
            perror("seteuid");
            return 1;
    }
    self = getpid();
	if (files->name && (files->items || all)) {
	    printf("DEVICE               USER        PID ACCESS COMMAND\n");
	    length = 0;
	    for (scan = files->name; *scan; scan++)
		if (*scan == '\\') length += printf("\\\\");
		else if (*scan > ' ' && *scan <= '~') {
			putchar(*scan);
			length++;
		    }
		    else length += printf("\\%03o",*scan);
		if (files->name_space)
		length += printf("/%s",files->name_space->name);
		while (length < NAME_FIELD) {
			putchar(' ');
			length++;
		}
		item = files->items;
		sprintf(path,PROC_BASE "/%d/stat",item->u.proc.pid);
		strcpy(comm,"???");
		if (f = fopen(path,"r")) {
			(void) fscanf(f,"%d (%[^)]",&dummy,comm);
			(void) fclose(f);
		}
		name = comm;
		uid = item->u.proc.uid;
		if (uid == UID_UNKNOWN) user = "???";
		else if (pw = getpwuid(uid)) user = pw->pw_name;
			else {
				sprintf(tmp,"%d",uid);
				user = tmp;
			}
		if (length > NAME_FIELD)
			printf("\n%*s",NAME_FIELD,"");
		printf(" %-8s ",user);
		printf("%6d %c%c%c%c%c  ",item->u.proc.pid,
			item->u.proc.ref_set & REF_FILE ? 'f' : '.',
			item->u.proc.ref_set & REF_ROOT ? 'r' : '.',
			item->u.proc.ref_set & REF_CWD ? 'c' : '.',
			item->u.proc.ref_set & REF_EXE ? 'e' : '.',
			(item->u.proc.ref_set & REF_MMAP) &&
			!(item->u.proc.ref_set & REF_EXE) ? 'm' : '.');
		if (name)
			for (scan = name; *scan; scan++)
				if (*scan == '\\') printf("\\\\");
			else if (*scan > ' ' && *scan <= '~')
				putchar(*scan);
			else printf("\\%03o",(unsigned char) *scan);
			putchar('\n');
	}
}
static void enter_item(const char *name,int flags,int sig_number,dev_t dev, ino_t ino,SPACE_DSC *name_space)
{
    static FILE_DSC *last = NULL;
    FILE_DSC *new;

    if (!(new = malloc(sizeof(FILE_DSC)))) {
	perror("malloc");
	exit(1);
    }
    if (last_named && !strcmp(last_named->name,name) &&
      last_named->name_space == name_space) new->name = NULL;
    else if (!(new->name = strdup(name))) {
	    perror("strdup");
	    exit(1);
	}
    new->flags = flags;
    new->sig_num = sig_number;
    new->items = NULL;
    new->next = NULL;
    new->dev = dev;
    new->ino = ino;
    new->name_space = name_space;
    if (last) last->next = new;
    else files = new;
    last = new;
    new->named = last_named;
    if (new->name) last_named = new;
}
extern void parse_args(char *argv)
{
    SPACE_DSC *name_space;
    char path[PATH_MAX+1];
    int flags,silent,sig_number,no_files;
    SPACE_DSC *this_name_space;
    struct stat st;
    char *here;

    flags = silent = 0;
    sig_number = SIGKILL;
    name_space = name_spaces;
    no_files = 1;
    flags |= FLAG_UID;
    no_files = 0;
    last_named = NULL;
    this_name_space = name_space;
    if (name_space != name_spaces || stat(argv,&st) < 0) {
		here = strchr(argv,'/');
		if (here && here != argv) {
		    for (this_name_space = name_spaces; this_name_space->name;
		      this_name_space++)
			if (!strcmp(here+1,this_name_space->name)) {
			    *here = 0;
			    break;
			}
		    if (!this_name_space->name) this_name_space = name_spaces;
		}
	    }
    if (this_name_space == name_spaces) {
		if (stat(argv,&st) < 0) {
		    perror(argv);
		    exit(0);
		}
		if (flags & FLAG_DEV)
		    if (S_ISBLK(st.st_mode)) st.st_dev = st.st_rdev;
		    else if (S_ISDIR(st.st_mode)) {
			    sprintf(path,"%s/.",argv);
			    if (stat(argv,&st) < 0) {
				perror(argv);
				exit(0);
			    }
			}
		if (!S_ISSOCK(st.st_mode) || (flags & FLAG_DEV))
		    enter_item(argv,flags,sig_number,st.st_dev,st.st_ino,NULL);
	    }
}


