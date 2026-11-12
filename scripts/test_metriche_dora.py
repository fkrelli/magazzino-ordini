import unittest
from datetime import datetime, timedelta, timezone
from unittest.mock import patch

from metriche_dora import GitHub, raccogli, report


FINE = datetime(2026, 11, 12, 12, tzinfo=timezone.utc)


class GitHubFinto:
    def elenco(self, percorso, parametri=None):
        if percorso == "/deployments":
            assert parametri == {"environment": "produzione"}
            return iter([{"id": 1, "sha": "abc"}, {"id": 2, "sha": "def"}])
        if percorso == "/deployments/1/statuses":
            return iter([
                {"state": "inactive", "created_at": "2026-11-12T10:00:00Z"},
                {"state": "success", "created_at": "2026-11-11T12:00:00Z"},
            ])
        if percorso == "/deployments/2/statuses":
            return iter([{"state": "failure", "created_at": "2026-11-11T12:00:00Z"}])
        if percorso == "/issues":
            return iter([
                {"title": "INCIDENT produzione", "created_at": "2026-10-01T12:00:00Z", "closed_at": "2026-11-10T12:00:00Z"},
                {"title": "incident aperto", "created_at": "2026-11-10T12:00:00Z", "closed_at": None},
                {"title": "incident PR", "created_at": "2026-11-10T12:00:00Z", "closed_at": "2026-11-11T12:00:00Z", "pull_request": {}},
                {"title": "altro", "created_at": "2026-11-10T12:00:00Z", "closed_at": "2026-11-11T12:00:00Z"},
            ])
        raise AssertionError(percorso)

    def get(self, percorso):
        assert percorso == "/commits/abc"
        return {"commit": {"committer": {"date": "2026-11-11T10:00:00Z"}}}


class MetricheDoraTest(unittest.TestCase):
    def test_report_deploy_e_incidenti(self):
        inizio, deploy, incidenti = raccogli(GitHubFinto(), FINE)
        testo = report(inizio, FINE, deploy, incidenti)
        self.assertEqual(inizio, FINE - timedelta(days=30))
        self.assertEqual(len(deploy), 1)
        self.assertEqual(len(incidenti), 1)
        self.assertIn("1 deployment riusciti", testo)
        self.assertIn("2.00 ore", testo)
        self.assertIn("960.00 ore", testo)
        self.assertIn("issue non sono collegate", testo)

    def test_report_senza_dati_non_inventa_tempi(self):
        testo = report(FINE - timedelta(days=30), FINE, [], [])
        self.assertIn("0 deployment riusciti", testo)
        self.assertEqual(testo.count("Non disponibile (nessun dato nel periodo)"), 2)

    def test_paginazione_non_perde_risultati(self):
        github = GitHub("https://api.github.com", "owner/repo", "token")
        with patch.object(github, "get", side_effect=[list(range(100)), [100]]) as get:
            self.assertEqual(list(github.elenco("/issues")), list(range(101)))
        self.assertEqual(get.call_args_list[1].args[1]["page"], 2)

    def test_confini_periodo_e_primo_successo(self):
        class GitHubConfini:
            def elenco(self, percorso, parametri=None):
                if percorso == "/deployments":
                    return iter([{"id": 1, "sha": "a"}, {"id": 2, "sha": "b"}])
                if percorso == "/deployments/1/statuses":
                    return iter([
                        {"state": "success", "created_at": "2026-10-13T12:00:00Z"},
                        {"state": "success", "created_at": "2026-11-11T12:00:00Z"},
                    ])
                if percorso == "/deployments/2/statuses":
                    return iter([{"state": "success", "created_at": "2026-11-12T12:00:00Z"}])
                return iter([
                    {"title": "incident incluso", "created_at": "2026-10-01T12:00:00Z", "closed_at": "2026-10-13T12:00:00Z"},
                    {"title": "incident escluso", "created_at": "2026-11-10T12:00:00Z", "closed_at": "2026-11-12T12:00:00Z"},
                ])

            def get(self, percorso):
                return {"commit": {"committer": {"date": "2026-10-13T11:00:00Z"}}}

        _, deploy, incidenti = raccogli(GitHubConfini(), FINE)
        self.assertEqual(len(deploy), 1)
        self.assertEqual(deploy[0][0], FINE - timedelta(days=30))
        self.assertEqual(len(incidenti), 1)

    def test_risposta_api_non_valida_non_produce_report(self):
        github = GitHub("https://api.github.com", "owner/repo", "token")
        with patch.object(github, "get", return_value={"message": "errore"}):
            with self.assertRaisesRegex(ValueError, "atteso un elenco"):
                list(github.elenco("/issues"))


if __name__ == "__main__":
    unittest.main()
