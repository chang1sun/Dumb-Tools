from pydiff import Diff

py = Diff()
d1, d2 = r"D://Test//testdir1", r"D://Test//testdir2"
if py.has_diffs(d1, d2):
    py.log()

