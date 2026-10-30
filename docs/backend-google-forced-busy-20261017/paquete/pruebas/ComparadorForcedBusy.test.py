"""Local regression only; never contacts Google."""
import importlib.util
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location('comparison', Path(__file__).parents[1] / 'ejecutor/comparar.py')
comparison = importlib.util.module_from_spec(spec)
spec.loader.exec_module(comparison)

class ArtificialContentionExclusion(unittest.TestCase):
    def test_artificial_time_excluded_but_normal_busy_retained(self):
        events = [
            {'action': 'testHoldLock', 'clientMs': 90000},
            {'action': 'testLockStatus', 'clientMs': 3000},
            {'action': 'stageRecordV3', 'scenario': 'FORCED_BUSY', 'clientMs': 30000, 'code': 'BUSY'},
            {'action': 'stageRecordV3', 'family': 'SIC', 'clientMs': 10, 'ok': True},
            {'action': 'commitRecordV3', 'family': 'SIC', 'clientMs': 20, 'ok': False, 'code': 'BUSY'},
        ]
        groups = comparison.summarize({'events': events})
        self.assertEqual(len(groups), 2)
        self.assertEqual(sum(g['clientMs']['total'] for g in groups.values()), 30)
        self.assertEqual(sum(g['busy'] for g in groups.values()), 1)

if __name__ == '__main__':
    unittest.main()
