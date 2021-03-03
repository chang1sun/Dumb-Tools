# by changnoob

"""
I found a useful tool named 'diffmerge', which implement swift function to find difference between
two folders or files. Thus implemented it with python3(The best language in the world!).
"""



import os
import datetime


class pydiff(object):

    _DATETIME = datetime.datetime.now().__format__("[%Y-%m-%d %H:%M:%S]")
    _NAME_DIFF = f'{_DATETIME}[Info]: Include different files:\n\n'
    _FILE_DIFF = f'{_DATETIME}[Info]: Find Files differences exist in:\n\n'
    _NO_DIFF = f'{_DATETIME}[Info]: No difference exist\n'
    """
            'log_output' record the latest log;
            'cache' is used to handle searching diffrence of mutiple-files;
            """
    # self._files_to_be_checked = []
    _log_output = []
    _cache = []

    def __repr__(self):
        return "<PyDiff object>"

    def log(self):
        """
        print the latest log
        :return: None
        """
        try:
            assert self._log_output
            print(''.join(self._log_output))
        except AssertionError:
            print(f"{self._DATETIME}[Info]: No latest log!")

    def _find(self, d1, d2, ignore_space) -> bool:
        d1, d2 = d1.replace("\\", '//'), d2.replace("\\", '//')
        with open(d1) as f1, open(d2) as f2:
            lines1 = [line for line in f1]
            lines2 = [line for line in f2]
            if len(lines1) - len(lines2) > 0:
                lines2.extend(['------Empty line------!' for i in range(len(lines1) - len(lines2))])
            elif len(lines2) - len(lines1) > 0:
                lines1.extend(['------Empty line------!' for i in range(len(lines2) - len(lines1))])
            for i, (l1, l2) in enumerate(zip(lines1, lines2), start=1):

                l1_f = l1.rstrip() + '\n' if ignore_space else l1.replace(' ', ' <sp> ')
                l2_f = l2.rstrip() + '\n' if ignore_space else l2.replace(' ', ' <sp> ')
                if l1_f != l2_f:
                    self._cache.append(f"{d1}: line {i}, {l1_f}{d2}: line {i}, {l2_f}\n\n")

        return True if self._cache else False

    def reset(self) -> bool:
        """
        clear log_output and restart logging
        :return: always return true
        """
        self._log_output = []
        # self._files_to_be_checked = []
        return True

    def has_diff(self, d1, d2, ignore_space=True) -> bool:
        """
        one of the main functions, to find difference between two files
        :param d1: Absolute path of first file
        :param d2: Absolute path of second file
        :param ignore_space: set True to ignore the space at the end of lines, default True
        :return: True if any difference exist, False otherwise.
        """
        if not os.path.exists(d1) or not os.path.exists(d2):
            raise CanNotFindPathError("Paths don't exist")

        if os.path.isdir(d1) or os.path.isdir(d2):
            raise NotTheRightTypeError("The paths should be files")

        if self._find(d1, d2, ignore_space):
            self._log_output.append(self._FILE_DIFF)
            self._log_output.extend(self._cache)
            self._cache = []
            return True
        self._log_output.append(self._NO_DIFF)
        return False

    def has_diffs(self, d1, d2, ignore_space=True) -> bool:
        """
        One of the main functions, to find difference between two directories
        :param d1: Absolute path of first directories
        :param d2: Absolute path of second directories
        :param ignore_space: set True to ignore the space at the end of lines, default True
        :return: True if any difference exist, False otherwise.
        """
        if not os.path.exists(d1) or not os.path.exists(d2):
            raise CanNotFindPathError("Paths don't exist")

        if os.path.isfile(d1) or os.path.isfile(d2):
            raise NotTheRightTypeError("The paths should be folders")

        if self._finds(d1, d2, ignore_space=ignore_space):
            if self._cache:
                self._log_output.append(self._FILE_DIFF)
                self._log_output.extend(self._cache)
                self._cache = []
            return True
        self._log_output.append(self._NO_DIFF)
        return False

    @staticmethod
    def _iterate(abs_d) -> set:
        """
        Use dfs to walk through every file and record their relative path
        """
        # Try os.walk, but not so ideal as I expected
        # dir_list = []
        # for root, dirs, files in os.walk(d):
        #     dir_list.extend(list(map(lambda x: root+x, files)))
        # return dir_list
        assert os.path.exists(abs_d)
        dirs = set()

        def iterate(d, path):
            for f in os.listdir(d+path):
                abs_path = d+path+f'\\{f}'
                if os.path.isdir(abs_path):
                    iterate(d, path+f'//{f}')
                else:
                    dirs.add(path+f'//{f}')
        iterate(abs_d, '')
        return dirs

    def _finds(self, d1, d2, ignore_space=True):
        dirs_1, dirs_2 = self._iterate(d1), self._iterate(d2)
        flag = False
        if dirs_1 != dirs_2:
            flag = True
            self._log_output.append(self._NAME_DIFF)
            dif1, dif2 = dirs_1-dirs_2, dirs_2-dirs_1
            if dif1:
                self._log_output.append(f"Only in {d1}:\n")
                self._log_output.append('\n'.join([f for f in dif1])+'\n\n')
            else:
                self._log_output.append(f"No extra files in {d1}\n\n")
            if dif2:
                self._log_output.append(f"Only in {d2}\n")
                self._log_output.append('\n'.join([f for f in dif2])+'\n\n')
            else:
                self._log_output.append(f"No extra files in {d2}\n\n")

        same_files = dirs_1 & dirs_2
        for f in same_files:
            if self._find(d1+f, d2+f, ignore_space=ignore_space):
                flag = True
        return flag


class NotTheRightTypeError(AssertionError):
    def __init__(self, message):
        self.message = message


class CanNotFindPathError(AssertionError):
    def __init__(self, message):
        self.message = message