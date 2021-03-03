import unittest
from os import path
from pydiff import pydiff, CanNotFindPathError, NotTheRightTypeError

class TestPydiff(unittest.TestCase):

    def try_has_diff(self, d1, d2, result=None, ignore_space=True):
        pydiff.has_diff(d1, d2, ignore_space=ignore_space)
        if result:
            self.assertEqual(''.join(pydiff._log_output), result)

    def try_has_diffs(self, d1, d2, result=None, ignore_space=True):
        pydiff.has_diffs(d1, d2, ignore_space=ignore_space)
        if result:
            self.assertEqual(''.join(pydiff._log_output), result)

    def test_ignore_space_true(self):
        d1 = r'D://Test//t3.txt'
        d2 = r'D://Test//t4.txt'
        self.try_has_diff(d1, d2, result=pydiff._NO_DIFF)

    def test_with_space_False(self):
        d1 = r'D:\Test\t3.txt'
        d2 = r'D:\Test\t4.txt'
        res = r"D://Test//t3.txt: line 1, I <sp> have <sp> a <sp> space <sp>  <sp>  <sp>  <sp>  <sp>  <sp>  <sp> "\
              + '\n' + r'D://Test//t4.txt: line 1, I <sp> have <sp> a <sp> space' + '\n\n'

        self.try_has_diff(d1, d2, result=pydiff._FILE_DIFF+res, ignore_space=False)

    def test_ignore_space_false(self):
        d1 = r'D:\Test\t5.txt'
        d2 = r'D:\Test\t6.txt'
        res_list = [
            pydiff._FILE_DIFF,
            r'D://Test//t5.txt: line 1, Excellent works,',
            '\n',
            r'D://Test//t6.txt: line 1, Excellent work,',
            '\n\n\n',
            r'D://Test//t5.txt: line 2, good job Chang!',
            '\n',
            r'D://Test//t6.txt: line 2, but still need improvement!',
            '\n\n\n'
        ]
        self.try_has_diff(d1, d2, result=''.join(res_list), ignore_space=True)

    def test_CanNotFindPathError(self):
        d1 = r'D:\Test\t100.txt'
        d2 = r'D:\Test\t6.txt'

        with self.assertRaises(CanNotFindPathError):
            self.try_has_diff(d1, d2)

    def test_NotTheRightTypeError(self):
        d1 = r'D:\Test\t5.txt'
        d2 = r'D:\Test\t6.txt'

        with self.assertRaises(NotTheRightTypeError):
            self.try_has_diffs(d1, d2)

    def test_empty_folders(self):
        d1 = r'D:\Test\t1'
        d2 = r'D:\Test\t2'
        self.try_has_diffs(d1, d2, result=pydiff._NO_DIFF)

    def test_folders_true(self):
        d1 = r'D:\Test\testdir3'
        d2 = r'D:\Test\testdir4'
        self.try_has_diffs(d1, d2, result=pydiff._NO_DIFF, ignore_space=True)

    def test_folders_false(self):
        d1 = r'D:\Test\testdir1'
        d2 = r'D:\Test\testdir2'

        res_list_ordered = [
            pydiff._NAME_DIFF,
            r'Only in D:\Test\testdir1:',
            '\n',
            r'//sample3.txt',
            '\n\n'
            r'No extra files in D:\Test\testdir2',
            '\n\n',
            pydiff._FILE_DIFF,
        ]

        res_list_not_ordered = [
            r'D://Test//testdir1//ssps//ggpa//sample1.txt: line 1, !'
            + '\n'
            + r'D://Test//testdir2//ssps//ggpa//sample1.txt: line 1, So what?'
            + '\n\n\n',

            r'D://Test//testdir1//ssps//ggpa//sample1.txt: line 2, Changed?'
            + '\n'
            + r'D://Test//testdir2//ssps//ggpa//sample1.txt: line 2, I change my mind'
            + '\n\n\n',

            r'D://Test//testdir1//ssps//ggpd//sample2.txt: line 1, NOthing!'
            + '\n'
            + r'D://Test//testdir2//ssps//ggpd//sample2.txt: line 1, not a shit'
            + '\n\n\n'
        ]
        pydiff.has_diffs(d1, d2, ignore_space=True)
        self.assertEqual(''.join(pydiff._log_output[:5]), ''.join(res_list_ordered))
        for elem in res_list_not_ordered:
            self.assertIn(elem, pydiff._log_output)


if __name__ == '__main__':
    unittest.main()
